"""Obilet şehir sayfasından origin/destination id okur. Sefer listesi ayrı sayfada."""

import re
from functools import lru_cache

import httpx

from app.settings import RouteTarget

_ORIGIN_ID = re.compile(r'"origin"\s*:\s*\{\s*"id"\s*:\s*(\d+)')
_DEST_ID = re.compile(r'"destination"\s*:\s*\{\s*"id"\s*:\s*(\d+)')
_TR = str.maketrans("ığüşöçİIĞÜŞÖÇ", "igusociigusoc")


def city_slug(city: str) -> str:
    # Türkçe İ/I önce ASCII'ye indirgenir; str.lower() birleşik nokta bırakmasın.
    value = city.strip().translate(_TR).lower()
    value = value.replace("\u0307", "")  # combining dot above (İ.lower yan etkisi)
    value = re.sub(r"[^a-z0-9]+", "-", value).strip("-")
    if not value:
        raise ValueError(f"Şehir adı boş: {city!r}")
    return value


def parse_route_ids(html: str) -> tuple[int, int]:
    origin = _ORIGIN_ID.search(html)
    dest = _DEST_ID.search(html)
    if not origin or not dest:
        raise ValueError("Şehir kimliği bulunamadı")
    return int(origin.group(1)), int(dest.group(1))


def resolve_route(origin: str, dest: str, client: httpx.Client | None = None) -> RouteTarget:
    origin_slug = city_slug(origin)
    dest_slug = city_slug(dest)
    origin_id, dest_id = _lookup_ids(origin_slug, dest_slug, client)
    return RouteTarget(
        origin=origin_slug,
        dest=dest_slug,
        obilet_origin_id=origin_id,
        obilet_dest_id=dest_id,
    )


def _lookup_ids(origin_slug: str, dest_slug: str, client: httpx.Client | None) -> tuple[int, int]:
    if client is None:
        return _cached_ids(origin_slug, dest_slug)
    return _fetch_ids(origin_slug, dest_slug, client)


@lru_cache(maxsize=256)
def _cached_ids(origin_slug: str, dest_slug: str) -> tuple[int, int]:
    with httpx.Client(timeout=30, follow_redirects=True, headers={"User-Agent": "Mozilla/5.0"}) as client:
        return _fetch_ids(origin_slug, dest_slug, client)


def _fetch_ids(origin_slug: str, dest_slug: str, client: httpx.Client) -> tuple[int, int]:
    url = f"https://www.obilet.com/otobus-bileti/{origin_slug}-{dest_slug}"
    response = client.get(url)
    response.raise_for_status()
    try:
        return parse_route_ids(response.text)
    except ValueError as ex:
        raise ValueError(f"{origin_slug}->{dest_slug}: {ex}") from ex
