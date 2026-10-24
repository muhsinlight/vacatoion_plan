"""Airbnb kiralık konut listesi — bot korumasına takılabilir; hatalar toleranslı."""

from __future__ import annotations

import logging
import re
from datetime import date
from decimal import Decimal
from urllib.parse import quote, urlencode

import httpx
from scrapling.parser import Selector

from app.bands import parse_tr_amount
from app.lodging_tiers import LodgingCard, select_tiers
from app.http_retry import request_with_retry
from app.providers.obilet_locations import city_slug
from app.settings import AirbnbSettings

log = logging.getLogger(__name__)

_RATING = re.compile(r"([0-9]+(?:[.,][0-9]+)?)\s*out of\s*5", re.I)
_PRICE = re.compile(r"(?:₺|TL)\s*([\d.,]+)|([\d.,]+)\s*(?:₺|TL)", re.I)


def search_url(
        settings: AirbnbSettings,
        city: str,
        check_in: date,
        check_out: date,
        adults: int = 2,
) -> str:
    slug = city_slug(city)
    path = settings.search_url.format(city_slug=quote(f"{slug}--Turkey"))
    query = urlencode({
        "checkin": check_in.isoformat(),
        "checkout": check_out.isoformat(),
        "adults": max(1, adults),
    })
    return f"{path}?{query}"


def parse_listings(html: str, source_url: str = "") -> list[LodgingCard]:
    page = Selector(html)
    cards: list[LodgingCard] = []
    for item in page.css("[data-testid='card-container']"):
        title = (
            item.css("[data-testid='listing-card-title']::text").get()
            or item.css("[data-testid='listing-card-name']::text").get()
            or ""
        ).strip()
        if not title:
            continue
        href = item.css("a::attr(href)").get()
        url = None
        if href:
            url = href if href.startswith("http") else f"https://www.airbnb.com.tr{href}"
        rating = _parse_rating(" ".join(item.css("[aria-label]::attr(aria-label)").getall()))
        price_text = " ".join(item.css("[data-testid='price-availability-row'] *::text").getall())
        price = _parse_price(price_text)
        if price is None or price <= 0:
            continue
        image = item.css("img::attr(src)").get()
        cards.append(LodgingCard(
            name=title,
            price_per_night=price,
            provider="airbnb",
            rating=rating,
            features=(),
            url=url,
            image_url=image,
        ))
    return cards


def scrape_rentals(
        settings: AirbnbSettings,
        city: str,
        check_in: date,
        check_out: date,
        adults: int = 2,
) -> tuple[list[LodgingCard], list[str]]:
    url = search_url(settings, city, check_in, check_out, adults)
    errors: list[str] = []
    try:
        with httpx.Client(
                timeout=settings.timeout_ms / 1000,
                follow_redirects=True,
                headers={
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Accept-Language": "tr-TR,tr;q=0.9",
                },
        ) as client:
            response = request_with_retry(client, "GET", url)
            if response.status_code >= 400:
                errors.append(f"{city}: kiralık listesi alınamadı ({response.status_code})")
                return [], errors
            cards = parse_listings(response.text, source_url=url)
    except Exception as ex:
        log.warning("Kiralık listesi alınamadı (%s)", city)
        return [], [f"{city}: {ex}"]

    if not cards:
        errors.append(f"{city}: kiralık ilan bulunamadı")
        return [], errors

    try:
        tiers = select_tiers(cards)
        return [tiers.budget, tiers.mid, tiers.premium], errors
    except ValueError as ex:
        return [], [f"{city}: {ex}"]


def _parse_rating(text: str) -> Decimal | None:
    m = _RATING.search(text or "")
    if not m:
        return None
    try:
        return Decimal(m.group(1).replace(",", ".")).quantize(Decimal("0.01"))
    except Exception:
        return None


def _parse_price(text: str) -> Decimal | None:
    m = _PRICE.search(text or "")
    if not m:
        return None
    raw = m.group(1) or m.group(2)
    try:
        return parse_tr_amount(raw + " TL")
    except ValueError:
        return None
