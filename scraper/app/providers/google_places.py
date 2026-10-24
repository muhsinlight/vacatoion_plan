"""Google Places API (New) ile yüksek puanlı restoranların kişi başı öğün aralığı.

Sayfa kazınmaz. Fiyat yalnız priceRange TRY ise alınır; fiyat seviyesi simgesinden tutar üretilmez.
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import ROUND_HALF_UP, Decimal
from functools import lru_cache
from pathlib import Path
from statistics import median

import httpx

from app.bands import CENTS, Band, confidence_for
from app.models import BudgetCategory, PriceObservation, PriceScopeType, PriceSourceType, PriceUnit
from app.providers.obilet_locations import city_slug
from app.settings import GooglePlacesSettings

ROOT = Path(__file__).resolve().parents[3]
CATALOG_PATH = ROOT / "api" / "src" / "main" / "resources" / "catalog-seed.json"
PROVINCES_PATH = ROOT / "contract" / "tr_provinces.json"
FIELD_MASK = "places.displayName,places.rating,places.userRatingCount,places.priceRange,places.googleMapsUri"
PROVIDER = "google-places"


class GoogleAuthError(RuntimeError):
    """Anahtar reddedildi; aynı anahtarla diğer illere devam edilmez."""


class GoogleQuotaError(RuntimeError):
    """Kota aşıldı; zamanlanmış tur durmalı."""


@dataclass(frozen=True)
class Restaurant:
    name: str
    rating: Decimal
    review_count: int
    meal_min: Decimal | None = None
    meal_max: Decimal | None = None
    url: str | None = None
    michelin: bool = False


@dataclass(frozen=True)
class FoodQuote:
    city_key: str
    places: list[Restaurant]
    daily: Band | None
    observation: PriceObservation | None
    errors: list[str]


def load_provinces(path: Path = CATALOG_PATH) -> list[tuple[str, str]]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    return [(item["cityKey"], item["displayName"]) for item in raw["provinces"]]


@lru_cache(maxsize=1)
def _aliases() -> dict[str, str]:
    if not PROVINCES_PATH.is_file():
        return {}
    raw = json.loads(PROVINCES_PATH.read_text(encoding="utf-8"))
    return {str(key): str(value) for key, value in (raw.get("aliases") or {}).items()}


def province_key(city: str) -> str:
    slug = city_slug(city)
    return _aliases().get(slug, slug)


def parse_money(node: dict | None) -> tuple[str, Decimal] | None:
    if not node:
        return None
    code = str(node.get("currencyCode") or "").upper()
    if not code:
        return None
    units = Decimal(str(node.get("units") or "0"))
    nanos = Decimal(node.get("nanos") or 0) / Decimal("1000000000")
    return code, (units + nanos).quantize(CENTS)


def _restaurant_sort_key(place: Restaurant) -> tuple:
    return (0 if place.michelin else 1, -place.rating, -place.review_count, place.name)


def merge_restaurant_lists(groups: list[list[Restaurant]], *, limit: int) -> list[Restaurant]:
    """Aynı isimli kayıtları birleştirir; Michelin işareti ve daha iyi puan korunur."""
    merged: dict[str, Restaurant] = {}
    for places in groups:
        for place in places:
            key = place.name.casefold().strip()
            if not key:
                continue
            prev = merged.get(key)
            if prev is None:
                merged[key] = place
            else:
                merged[key] = _combine_restaurants(prev, place)
    ordered = sorted(merged.values(), key=_restaurant_sort_key)
    return ordered[: max(0, limit)]


def _combine_restaurants(left: Restaurant, right: Restaurant) -> Restaurant:
    pick = left if left.rating >= right.rating else right
    other = right if pick is left else left
    meal_min = pick.meal_min if pick.meal_min is not None else other.meal_min
    meal_max = pick.meal_max if pick.meal_max is not None else other.meal_max
    url = pick.url or other.url
    return Restaurant(
        name=pick.name,
        rating=max(left.rating, right.rating),
        review_count=max(left.review_count, right.review_count),
        meal_min=meal_min,
        meal_max=meal_max,
        url=url,
        michelin=left.michelin or right.michelin,
    )


def select_restaurants(
        payload: dict,
        *,
        min_rating: float,
        min_reviews: int,
        limit: int,
        mark_michelin: bool = False,
) -> list[Restaurant]:
    """Puan ve yorum eşiğinin altındakiler elenir. TRY fiyatı yoksa restoran yine listelenir.

    Kalanlar önce yıldıza, eşitlikte yorum sayısına göre sıralanır.
    """
    floor = Decimal(str(min_rating))
    chosen: list[Restaurant] = []
    for place in payload.get("places") or []:
        rating_raw = place.get("rating")
        reviews = place.get("userRatingCount") or 0
        if rating_raw is None or reviews < min_reviews:
            continue
        rating = Decimal(str(rating_raw)).quantize(Decimal("0.1"))
        if rating < floor:
            continue
        name = ((place.get("displayName") or {}).get("text") or "").strip()
        if not name:
            continue
        meal = _meal_range(place.get("priceRange") or {})
        low, high = meal if meal is not None else (None, None)
        chosen.append(Restaurant(
            name=name,
            rating=rating,
            review_count=int(reviews),
            meal_min=low,
            meal_max=high,
            url=place.get("googleMapsUri"),
            michelin=mark_michelin or "michelin" in name.casefold(),
        ))
    chosen.sort(key=_restaurant_sort_key)
    return chosen[: max(0, limit)]


def daily_band(places: list[Restaurant], meals_per_day: Decimal) -> Band:
    """Google aralığı bir öğündür. Gün = öğün × meals_per_day (öğle, akşam ve daha ucuz kahvaltı)."""
    priced = [place for place in places if place.meal_min is not None and place.meal_max is not None]
    if not priced:
        raise ValueError("TRY fiyatı olan restoran yok")
    if meals_per_day <= 0:
        raise ValueError("meals_per_day pozitif olmalı")
    lows = [place.meal_min for place in priced]
    highs = [place.meal_max for place in priced]
    mids = [((place.meal_min + place.meal_max) / 2) for place in priced]
    factor = Decimal(meals_per_day)

    def scale(value: Decimal) -> Decimal:
        return (value * factor).quantize(CENTS, rounding=ROUND_HALF_UP)

    low = scale(min(lows))
    high = scale(max(highs))
    typical = scale(Decimal(median(mids)))
    if typical < low:
        typical = low
    if typical > high:
        typical = high
    return Band(
        min=low,
        typical=typical,
        max=high,
        sample_size=len(priced),
        confidence=confidence_for(len(priced)),
    )


def build_observation(city_key: str, band: Band, checked_at: datetime) -> PriceObservation:
    return PriceObservation(
        category=BudgetCategory.FOOD,
        scope_type=PriceScopeType.DESTINATION,
        scope_key=city_key,
        unit=PriceUnit.PER_DAY,
        min=band.min,
        typical=band.typical,
        max=band.max,
        source_type=PriceSourceType.OFFICIAL_API,
        provider=PROVIDER,
        source_url="https://places.googleapis.com",
        sample_size=band.sample_size,
        confidence=band.confidence,
        checked_at=checked_at,
    )


def fetch_city(settings: GooglePlacesSettings, city: str, client: httpx.Client) -> FoodQuote:
    key = province_key(city)
    if not settings.api_key.strip():
        return FoodQuote(key, [], None, None, ["GOOGLE_PLACES_API_KEY yok"])
    city_label = city.strip()
    payload = _search(settings, city_label, client, f"restoran {city_label}, Türkiye")
    general = select_restaurants(
        payload,
        min_rating=settings.min_rating,
        min_reviews=settings.min_reviews,
        limit=settings.result_limit,
    )
    michelin: list[Restaurant] = []
    if settings.include_michelin_search:
        michelin_payload = _search(
            settings,
            city_label,
            client,
            f"Michelin yıldızlı restoran {city_label}, Türkiye",
        )
        michelin = select_restaurants(
            michelin_payload,
            min_rating=settings.michelin_min_rating,
            min_reviews=settings.michelin_min_reviews,
            limit=settings.result_limit,
            mark_michelin=True,
        )
    places = merge_restaurant_lists([general, michelin], limit=settings.result_limit)
    if not places:
        return FoodQuote(key, [], None, None, [f"{key}: yeterli puanlı restoran yok"])
    priced = [place for place in places if place.meal_min is not None and place.meal_max is not None]
    if not priced:
        return FoodQuote(
            key,
            places,
            None,
            None,
            [f"{key}: TRY fiyatı yok; restoran adları katalog tahminiyle döndü"],
        )
    band = daily_band(priced, Decimal(settings.meals_per_day))
    checked_at = datetime.now(timezone.utc)
    return FoodQuote(key, places, band, build_observation(key, band, checked_at), [])


def _meal_range(price_range: dict) -> tuple[Decimal, Decimal] | None:
    start = parse_money(price_range.get("startPrice"))
    if start is None or start[0] != "TRY" or start[1] <= 0:
        return None
    end = parse_money(price_range.get("endPrice"))
    high = start[1]
    if end is not None:
        if end[0] != "TRY" or end[1] < start[1]:
            return None
        high = end[1]
    return start[1], high


def _search(settings: GooglePlacesSettings, city: str, client: httpx.Client, text_query: str) -> dict:
    page_size = min(20, max(1, settings.page_size))
    body = {
        "textQuery": text_query,
        "includedType": "restaurant",
        "strictTypeFiltering": True,
        "languageCode": "tr",
        "regionCode": "TR",
        "pageSize": page_size,
        "minRating": settings.min_rating,
        "rankPreference": "RELEVANCE",
    }
    headers = {
        "X-Goog-Api-Key": settings.api_key,
        "X-Goog-FieldMask": FIELD_MASK,
    }
    response = client.post(settings.search_url, headers=headers, json=body)
    if response.status_code in (401, 403):
        raise GoogleAuthError("Google Places anahtarı reddedildi")
    if response.status_code == 429:
        raise GoogleQuotaError("Google Places kotası doldu")
    response.raise_for_status()
    return response.json()
