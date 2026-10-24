"""OpenStreetMap restoran listesi. Öğün fiyatı üretmez; tutar katalog bandında kalır."""

from __future__ import annotations

import logging
from dataclasses import dataclass

import httpx

from app.http_retry import request_with_retry
from app.providers.google_places import province_key
from app.settings import OsmSettings

log = logging.getLogger(__name__)

PROVIDER = "openstreetmap"
# İl sınırı (province) Nominatim'de sık ilk sırada gelir; merkezi şehirden uzak olabilir.
_SETTLEMENT = frozenset({
    "city", "town", "municipality", "village", "borough",
    "suburb", "quarter", "neighbourhood", "hamlet",
})
_REGION = frozenset({"province", "state", "region", "country"})


@dataclass(frozen=True)
class OsmPlace:
    name: str
    url: str


@dataclass(frozen=True)
class OsmFoodResult:
    city_key: str
    places: list[OsmPlace]
    errors: list[str]


def fetch_places(settings: OsmSettings, city: str, client: httpx.Client) -> OsmFoodResult:
    key = province_key(city)
    label = city.strip()
    try:
        point = _geocode(settings, label, client)
    except httpx.HTTPError as ex:
        log.exception("Şehir konumu alınamadı: %s", key)
        return OsmFoodResult(key, [], [f"{key}: {ex}"])
    if point is None:
        return OsmFoodResult(key, [], [f"{key}: şehir konumu yok"])
    lat, lon = point
    try:
        elements = _restaurants(settings, lat, lon, client)
    except httpx.HTTPError as ex:
        log.exception("Restoran listesi alınamadı: %s", key)
        return OsmFoodResult(key, [], [f"{key}: {ex}"])
    places = _named_places(elements, settings.result_limit)
    wide = max(settings.radius_m * 2, 15_000)
    if len(places) < 3 and wide > settings.radius_m:
        try:
            wider = _restaurants(settings, lat, lon, client, wide)
        except httpx.HTTPError:
            log.warning("Geniş restoran araması alınamadı: %s", key)
            wider = []
        if wider:
            places = _named_places(wider, settings.result_limit)
    if not places:
        return OsmFoodResult(key, [], [f"{key}: restoran bulunamadı"])
    log.info("%s: %d restoran", key, len(places))
    return OsmFoodResult(key, places, [])


def _place_point(rows: list[dict]) -> tuple[float, float] | None:
    """Yerleşimi seçer. İl poligonunun merkezi 6 km aramayı boşaltmasın."""
    if not rows:
        return None
    chosen = next((row for row in rows if row.get("addresstype") in _SETTLEMENT), None)
    if chosen is None:
        chosen = next((row for row in rows if row.get("addresstype") not in _REGION), None)
    if chosen is None:
        chosen = rows[0]
    return float(chosen["lat"]), float(chosen["lon"])


def _geocode(settings: OsmSettings, city: str, client: httpx.Client) -> tuple[float, float] | None:
    # Serbest "Van, Türkiye" araması banka ve sınır noktalarını da getiriyor.
    response = request_with_retry(
        client,
        "GET",
        settings.nominatim_url,
        params={
            "city": city,
            "country": "Türkiye",
            "format": "jsonv2",
            "limit": 5,
        },
        headers=_headers(settings),
    )
    response.raise_for_status()
    point = _place_point(response.json())
    if point is not None:
        return point
    response = request_with_retry(
        client,
        "GET",
        settings.nominatim_url,
        params={
            "q": city,
            "format": "jsonv2",
            "limit": 5,
            "countrycodes": "tr",
        },
        headers=_headers(settings),
    )
    response.raise_for_status()
    return _place_point(response.json())


_OVERPASS_RETRY = {429, 502, 503, 504}
_FOOD_AMENITIES = "restaurant|cafe|fast_food"


def _restaurants(
        settings: OsmSettings,
        lat: float,
        lon: float,
        client: httpx.Client,
        radius_m: int | None = None,
) -> list[dict]:
    radius = settings.radius_m if radius_m is None else radius_m
    cap = max(settings.result_limit * 4, settings.result_limit)
    query = (
        f"[out:json][timeout:25];"
        f"(nwr[\"amenity\"~\"{_FOOD_AMENITIES}\"](around:{radius},{lat},{lon}););"
        f"out center {cap};"
    )
    body = query.encode("utf-8")
    last: Exception | None = None
    for url in settings.overpass_endpoints():
        try:
            response = client.post(url, content=body, headers=_headers(settings), timeout=20.0)
        except httpx.HTTPError as ex:
            log.warning("Overpass %s ulaşılamadı: %s", url, ex)
            last = ex
            continue
        if response.status_code in _OVERPASS_RETRY:
            log.warning("Overpass %s yanıtı %s", url, response.status_code)
            last = httpx.HTTPStatusError(
                f"Overpass {response.status_code}",
                request=response.request,
                response=response,
            )
            continue
        response.raise_for_status()
        payload = response.json()
        return list(payload.get("elements") or [])
    if last is not None:
        raise last
    return []


def _named_places(elements: list[dict], limit: int) -> list[OsmPlace]:
    places: list[OsmPlace] = []
    seen: set[str] = set()
    for element in elements:
        tags = element.get("tags") or {}
        name = str(tags.get("name") or tags.get("name:tr") or "").strip()
        if len(name) < 2:
            continue
        key = name.casefold()
        if key in seen:
            continue
        url = _osm_url(str(element.get("type") or ""), element.get("id"))
        if not url:
            continue
        seen.add(key)
        places.append(OsmPlace(name=name, url=url))
        if len(places) >= limit:
            break
    return places


def _osm_url(osm_type: str, osm_id: object) -> str:
    if osm_type not in ("node", "way", "relation") or not isinstance(osm_id, int):
        return ""
    return f"https://www.openstreetmap.org/{osm_type}/{osm_id}"


def _headers(settings: OsmSettings) -> dict[str, str]:
    return {"User-Agent": settings.user_agent, "Accept-Language": "tr"}
