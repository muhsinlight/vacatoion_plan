"""OpenStreetMap restoran listesi. Öğün fiyatı ve FOOD_PER_PERSON_DAY gözlemi üretmez."""

from __future__ import annotations

import logging

import httpx

from app.models import FoodPlaceDto, FoodSearchRequest, FoodSearchResponse
from app.providers import osm_food
from app.providers.google_places import province_key
from app.settings import Settings

log = logging.getLogger(__name__)


def search_food(settings: Settings, request: FoodSearchRequest) -> FoodSearchResponse:
    city = (request.city or "").strip()
    if not city:
        return FoodSearchResponse(city_key="", errors=["city gerekli"])
    key = province_key(city)
    osm = settings.providers.osm
    try:
        with httpx.Client(timeout=osm.timeout_seconds) as client:
            result = osm_food.fetch_places(osm, city, client)
    except httpx.HTTPError as ex:
        log.warning("Restoran listesi alınamadı: %s", key)
        return FoodSearchResponse(city_key=key, errors=[f"{key}: {ex}"])
    places = [FoodPlaceDto(name=place.name, url=place.url) for place in result.places]
    return FoodSearchResponse(
        city_key=result.city_key or key,
        provider=osm_food.PROVIDER if places else None,
        places=places,
        errors=list(result.errors),
    )
