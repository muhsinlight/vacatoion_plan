"""X-Scraper-Key kontrolü. Kural API'deki ScraperKeys ile aynı: en az 32 karakter, sabit zamanlı karşılaştırma."""

import hmac

from fastapi import Header, HTTPException

from app.settings import get_settings

HEADER = "X-Scraper-Key"
MIN_KEY_LENGTH = 32


def is_usable(key: str | None) -> bool:
    return key is not None and len(key.strip()) >= MIN_KEY_LENGTH


def key_matches(expected: str | None, provided: str | None) -> bool:
    if not is_usable(expected) or provided is None:
        return False
    return hmac.compare_digest(expected.strip().encode(), provided.strip().encode())


def require_trigger_key(x_scraper_key: str | None = Header(default=None)) -> None:
    expected = get_settings().server.trigger_key
    if not is_usable(expected):
        raise HTTPException(status_code=503, detail="Scraper tetikleme anahtarı yapılandırılmamış")
    if not key_matches(expected, x_scraper_key):
        raise HTTPException(status_code=401, detail="Geçersiz scraper anahtarı")
