"""API'deki scrape_targets kayıtları (GET /targets) -> POST /ingest. Şu an yalnızca JSON_MIN_MAX parser'ı var."""

from datetime import datetime, timezone
from decimal import Decimal
from urllib.parse import urlsplit

import httpx

from app.models import ScrapeIngestRequest, ScrapeTarget


def parse_json_min_max(payload: dict) -> tuple[Decimal, Decimal, str]:
    return Decimal(str(payload["min"])), Decimal(str(payload["max"])), payload.get("currency") or "TRY"


PARSERS = {"JSON_MIN_MAX": parse_json_min_max}


def check_source_url(url: str, allowed_hosts: list[str]) -> None:
    """Veritabanından gelen adres yalnız izinli alana https ile gider; iç ağa veya başka şemaya çıkmaz."""
    parts = urlsplit(url)
    host = (parts.hostname or "").lower().rstrip(".")
    allowed = {h.lower().rstrip(".") for h in allowed_hosts}
    if parts.scheme != "https":
        raise ValueError(f"Yalnız https kabul edilir: {url}")
    if parts.username or parts.password:
        raise ValueError(f"Adreste kullanıcı bilgisi olamaz: {url}")
    if parts.port not in (None, 443):
        raise ValueError(f"Standart dışı port: {url}")
    if host not in allowed:
        raise ValueError(f"İzinli olmayan alan: {host or url}")


def fetch_target(target: ScrapeTarget, client: httpx.Client, allowed_hosts: list[str]) -> ScrapeIngestRequest:
    parser = PARSERS.get(target.parser_type)
    if parser is None:
        raise ValueError(f"Desteklenmeyen parser: {target.parser_type}")
    check_source_url(target.source_url, allowed_hosts)
    response = client.get(target.source_url, follow_redirects=False)
    response.raise_for_status()
    low, high, currency = parser(response.json())
    return ScrapeIngestRequest(
        target_id=target.id,
        min=low,
        max=high,
        currency=currency,
        fetched_at=datetime.now(timezone.utc),
    )
