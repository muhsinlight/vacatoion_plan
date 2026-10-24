"""Obilet otel SEO sayfasından popüler oteller ve gecelik fiyat bandı."""

from __future__ import annotations

import json
import logging
import re
from dataclasses import dataclass
from datetime import date
from decimal import Decimal
from urllib.parse import urlencode

import httpx
from scrapling.parser import Selector

from app.bands import parse_tr_amount
from app.lodging_tiers import LodgingCard, select_tiers
from app.http_retry import request_with_retry
from app.providers.obilet_locations import city_slug
from app.settings import ObiletHotelSettings

log = logging.getLogger(__name__)

_PAGE_MODEL = re.compile(
    r'<script[^>]*id=["\']page-model["\'][^>]*>(.*?)</script>',
    re.S | re.I,
)
_AVG_PRICE = re.compile(r'"averagePrice"\s*:\s*([0-9.]+)')
_PRICE_RANGE = re.compile(r'"pagePriceRange"\s*:\s*"([^"]+)"')
_RANGE_AMOUNT = re.compile(r"(\d[\d.\s]*)\s*TL", re.I)
_POPULAR_START = '"popularHotels"'
_HOTEL_ORIGIN = re.compile(
    r'"origin"\s*:\s*\{[^{}]{0,500}?"externalId"\s*:\s*"?(?P<external>\d+)"?',
    re.S,
)
_HOTEL_ORIGIN_ID = re.compile(r'"id"\s*:\s*"?(?P<id>\d+)"?')


CITY_RANGE_NAME = "Obilet şehir aralığı"


@dataclass(frozen=True)
class HotelSearchResult:
    cards: list[LodgingCard]
    min_price: Decimal | None
    typical_price: Decimal | None
    max_price: Decimal | None
    source_url: str


@dataclass(frozen=True)
class HotelScrape:
    cards: list[LodgingCard]
    errors: list[str]
    estimated: bool = False
    band_min: Decimal | None = None
    band_typical: Decimal | None = None
    band_max: Decimal | None = None


def search_url(
        settings: ObiletHotelSettings,
        city: str,
        check_in: date,
        check_out: date,
        adults: int = 2,
) -> str:
    slug = city_slug(city)
    base = settings.search_url.format(city_slug=f"{slug}-otelleri")
    query = urlencode({
        "checkin": check_in.isoformat(),
        "checkout": check_out.isoformat(),
        "adult": max(1, adults),
    })
    return f"{base}?{query}"


def parse_hotels(html: str, source_url: str = "") -> HotelSearchResult:
    """Önce gömülü page-model / JSON, yoksa DOM kartlarından okur."""
    model = _extract_page_model(html)
    cards: list[LodgingCard] = []
    min_price: Decimal | None = None
    typical_price: Decimal | None = None
    max_price: Decimal | None = None

    if model:
        typical_raw = model.get("averagePrice")
        if typical_raw is not None:
            typical_price = Decimal(str(typical_raw)).quantize(Decimal("0.01"))
        range_text = model.get("pagePriceRange") or ""
        min_price, max_price = _range_bounds(range_text)
        for hotel in model.get("popularHotels") or []:
            card = _card_from_json(hotel, settings_base=source_url)
            if card:
                cards.append(card)

    if not cards:
        cards = _cards_from_dom(html)

    if min_price is None or max_price is None:
        match = _PRICE_RANGE.search(html)
        low, high = _range_bounds(match.group(1) if match else "")
        min_price = min_price or low
        max_price = max_price or high
    if typical_price is None:
        m = _AVG_PRICE.search(html)
        if m:
            typical_price = Decimal(m.group(1)).quantize(Decimal("0.01"))

    if not cards:
        # JSON'dan kart çıkmadıysa popularHotels dizisini ham HTML'den dene
        cards = _cards_from_embedded_array(html)

    return HotelSearchResult(
        cards=cards,
        min_price=min_price,
        typical_price=typical_price,
        max_price=max_price,
        source_url=source_url,
    )


def scrape_hotels(
        settings: ObiletHotelSettings,
        city: str,
        check_in: date,
        check_out: date,
        adults: int = 2,
        accommodation_hint: str | None = None,
) -> HotelScrape:
    """Şehir SEO sayfasını çeker; pansiyon için ada göre süzülür."""
    url = search_url(settings, city, check_in, check_out, adults)
    errors: list[str] = []
    try:
        with httpx.Client(
                timeout=settings.timeout_ms / 1000,
                follow_redirects=True,
                headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"},
        ) as client:
            response = request_with_retry(client, "GET", url)
            response.raise_for_status()
            html = response.text
            result = parse_hotels(html, source_url=url)
            offer_cards = _cards_from_offer_search(
                client, html, city, check_in, check_out, adults,
            )
    except Exception as ex:
        log.exception("Otel fiyatı alınamadı: %s", city)
        return HotelScrape([], [f"{city}: {ex}"])

    cards = offer_cards or result.cards
    if accommodation_hint and accommodation_hint.upper() == "PENSION":
        filtered = [c for c in cards if _looks_like_pension(c.name)]
        if filtered:
            cards = filtered
        else:
            errors.append(f"{city}: pansiyon adı bulunamadı; tüm oteller kullanıldı")

    if not cards:
        errors.append(f"{city}: otel kartı yok")
        return HotelScrape([], errors)

    if not any(c.price_per_night > 0 for c in cards):
        estimate = _city_range(result)
        if estimate is None:
            errors.append(f"{city}: otel kartında gecelik fiyat yok")
            return HotelScrape([], errors)
        errors.append(f"{city}: otel kartında gecelik fiyat yok; şehir aralığı kullanıldı")
        low, typical, high = estimate
        return HotelScrape(
            [LodgingCard(name=CITY_RANGE_NAME, price_per_night=typical, provider="obilet", url=result.source_url)],
            errors,
            estimated=True,
            band_min=low,
            band_typical=typical,
            band_max=high,
        )

    try:
        tiers = select_tiers(cards)
        return HotelScrape([tiers.budget, tiers.mid, tiers.premium], errors)
    except ValueError as ex:
        return HotelScrape([], [f"{city}: {ex}"])


def _cards_from_offer_search(
        client: httpx.Client,
        html: str,
        city: str,
        check_in: date,
        check_out: date,
        adults: int,
) -> list[LodgingCard]:
    """SEO kartında gecelik fiyat yok. Tarihli arama JSON'u otel başına tutar döner."""
    origin = _hotel_origin(html)
    if origin is None or not hasattr(client, "post"):
        return []
    origin_id, external_id = origin
    slug = city_slug(city)
    # Obilet geçmiş giriş tarihine 500 döner. Geçmiş giriş bugüne çekilir; çıkış durur.
    search_in = check_in if check_in >= date.today() else date.today()
    search_out = check_out
    if not search_out > search_in:
        return []
    dates = f"{search_in:%Y%m%d}-{search_out:%Y%m%d}"
    pax = f"{max(1, adults)}ad"
    list_url = f"https://www.obilet.com/oteller/{slug}-{origin_id}-{external_id}-2/{dates}/{pax}"
    json_url = f"https://www.obilet.com/json/oteller/{external_id}/{dates}/{pax}/2"
    try:
        client.get(list_url)
        response = client.post(
            json_url,
            json={},
            headers={
                "Accept": "application/json",
                "Referer": list_url,
                "Content-Type": "application/json",
            },
        )
        response.raise_for_status()
        payload = response.json()
    except Exception:
        log.warning("Otel arama fiyatı alınamadı: %s", city)
        return []
    if not isinstance(payload, dict):
        return []
    return cards_from_offer_payload(
        payload,
        city_slug_value=slug,
        origin_id=origin_id,
        external_id=external_id,
        dates=dates,
        pax=pax,
    )


def cards_from_offer_payload(
        payload: dict,
        *,
        city_slug_value: str,
        origin_id: str,
        external_id: str,
        dates: str,
        pax: str,
) -> list[LodgingCard]:
    data = payload.get("data")
    if not isinstance(data, dict):
        return []
    cards: list[LodgingCard] = []
    for hotel in data.get("searchHotel") or []:
        if not isinstance(hotel, dict):
            continue
        name = (hotel.get("name") or "").strip()
        nightly = _cheapest_nightly(hotel.get("offers") or [])
        if not name or nightly is None:
            continue
        hotel_id = hotel.get("id")
        url = None
        if hotel_id is not None:
            hotel_slug = city_slug(name)
            url = (
                "https://www.obilet.com/oteller/"
                f"{hotel_slug}-{hotel_id}_{city_slug_value}-{origin_id}-{external_id}-2/{dates}/{pax}"
            )
        board = _board_name(hotel.get("offers") or [])
        rating = hotel.get("averageScore") or hotel.get("rating")
        stars = hotel.get("stars") if hotel.get("stars") is not None else hotel.get("star")
        cards.append(LodgingCard(
            name=name,
            price_per_night=nightly,
            provider="obilet",
            stars=int(stars) if isinstance(stars, int) or (isinstance(stars, str) and stars.isdigit()) else None,
            rating=Decimal(str(rating)).quantize(Decimal("0.1")) if rating is not None else None,
            board_type=board,
            url=url,
            image_url=hotel.get("photoUrl") or hotel.get("showCasePhoto"),
        ))
    return cards


def _cheapest_nightly(offers: list) -> Decimal | None:
    prices: list[Decimal] = []
    for offer in offers:
        if not isinstance(offer, dict):
            continue
        price = offer.get("price") or {}
        amount = price.get("amount") if isinstance(price, dict) else None
        nights = offer.get("night") or 0
        if amount is None or nights <= 0:
            continue
        nightly = (Decimal(str(amount)) / Decimal(str(nights))).quantize(Decimal("0.01"))
        if nightly > 0:
            prices.append(nightly)
    return min(prices) if prices else None


def _board_name(offers: list) -> str | None:
    for offer in offers:
        if not isinstance(offer, dict):
            continue
        board = offer.get("boardItem") or {}
        if isinstance(board, dict) and board.get("name"):
            return str(board["name"])
    return None


def _hotel_origin(html: str) -> tuple[str, str] | None:
    match = _HOTEL_ORIGIN.search(html)
    if not match:
        return None
    span = match.group(0)
    origin_id = _HOTEL_ORIGIN_ID.search(span)
    if not origin_id:
        return None
    return origin_id.group("id"), match.group("external")


def _extract_page_model(html: str) -> dict | None:
    m = _PAGE_MODEL.search(html)
    if m:
        try:
            return json.loads(m.group(1))
        except json.JSONDecodeError:
            pass
    # Canlı Obilet sayfasında model büyük script içinde
    hotels = _extract_json_array(html, _POPULAR_START)
    if hotels is None:
        return None
    avg = _AVG_PRICE.search(html)
    rng = _PRICE_RANGE.search(html)
    return {
        "popularHotels": hotels,
        "averagePrice": float(avg.group(1)) if avg else None,
        "pagePriceRange": rng.group(1) if rng else None,
    }


def _extract_json_array(html: str, key: str) -> list | None:
    start = html.find(key)
    if start < 0:
        return None
    start = html.find("[", start)
    if start < 0:
        return None
    depth = 0
    for i, ch in enumerate(html[start:], start):
        if ch == "[":
            depth += 1
        elif ch == "]":
            depth -= 1
            if depth == 0:
                try:
                    return json.loads(html[start:i + 1])
                except json.JSONDecodeError:
                    return None
    return None


def _cards_from_embedded_array(html: str) -> list[LodgingCard]:
    hotels = _extract_json_array(html, _POPULAR_START) or []
    cards = []
    for hotel in hotels:
        card = _card_from_json(hotel)
        if card:
            cards.append(card)
    return cards


def _card_from_json(hotel: dict, settings_base: str = "") -> LodgingCard | None:
    name = (hotel.get("name") or "").strip()
    if not name:
        return None
    slug = hotel.get("b2CUrl") or hotel.get("url") or ""
    url = None
    if slug:
        url = slug if str(slug).startswith("http") else f"https://www.obilet.com/otel/{slug}"
    features = tuple(
        f.get("name") for f in (hotel.get("hotelPopularFacilities") or [])
        if isinstance(f, dict) and f.get("name")
    )
    stars = hotel.get("star")
    rating = hotel.get("averageScore") or hotel.get("rating")
    price = hotel.get("pricePerNight") or hotel.get("minPrice") or hotel.get("price") or 0
    return LodgingCard(
        name=name,
        price_per_night=Decimal(str(price)).quantize(Decimal("0.01")),
        provider="obilet",
        stars=int(stars) if stars is not None else None,
        rating=Decimal(str(rating)).quantize(Decimal("0.1")) if rating is not None else None,
        board_type=hotel.get("themeName") or hotel.get("boardType"),
        features=features,
        url=url,
        image_url=hotel.get("showCasePhoto") or hotel.get("imageUrl"),
    )


def _cards_from_dom(html: str) -> list[LodgingCard]:
    page = Selector(html)
    cards: list[LodgingCard] = []
    for item in page.css(".popular-hotel-card"):
        name = (item.css(".popular-hotel-card__name::text").get()
                or item.css("a.popular-hotel-card__name::text").get()
                or "").strip()
        if not name:
            continue
        href = item.css("a.popular-hotel-card__name::attr(href)").get() or item.css("a::attr(href)").get()
        url = None
        if href:
            url = href if href.startswith("http") else f"https://www.obilet.com{href}"
        features = tuple(
            t.strip() for t in item.css(".hotel-features__item::text").getall() if t and t.strip()
        )
        rating_text = (item.css(".hotel-review__badge::text").get() or "").strip()
        rating = None
        if rating_text:
            try:
                rating = Decimal(rating_text.replace(",", ".")).quantize(Decimal("0.1"))
            except Exception:
                rating = None
        cards.append(LodgingCard(
            name=name,
            price_per_night=Decimal("0"),
            provider="obilet",
            rating=rating,
            features=features,
            url=url,
        ))
    return cards


def _city_range(result: HotelSearchResult) -> tuple[Decimal, Decimal, Decimal] | None:
    typical = result.typical_price
    if typical is None or typical <= 0:
        return None
    low = result.min_price if result.min_price and result.min_price > 0 else typical
    high = result.max_price if result.max_price and result.max_price > 0 else typical
    if low > high:
        low, high = high, low
    if typical < low:
        typical = low
    if typical > high:
        typical = high
    return low, typical, high


def _range_bounds(text: str) -> tuple[Decimal | None, Decimal | None]:
    amounts: list[Decimal] = []
    for match in _RANGE_AMOUNT.finditer(text or ""):
        try:
            amounts.append(parse_tr_amount(match.group(1) + " TL"))
        except ValueError:
            continue
    if not amounts:
        return None, None
    if len(amounts) == 1:
        return amounts[0], None
    return min(amounts), max(amounts)


def _parse_range_min(text: str) -> Decimal | None:
    if not text:
        return None
    m = _RANGE_AMOUNT.search(text)
    if not m:
        return None
    try:
        return parse_tr_amount(m.group(1) + " TL")
    except ValueError:
        return None


def _looks_like_pension(name: str) -> bool:
    lower = name.casefold()
    return any(token in lower for token in ("pansiyon", "pension", "hostel", "konukevi", "guesthouse"))
