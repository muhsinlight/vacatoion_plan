from datetime import date
from decimal import Decimal
from pathlib import Path

from app.providers.airbnb_rentals import parse_listings, search_url
from app.settings import AirbnbSettings

FIXTURE = Path(__file__).parent / "fixtures" / "airbnb_listings.html"


def test_search_url():
    url = search_url(
        AirbnbSettings(),
        "İstanbul",
        date(2026, 10, 29),
        date(2026, 11, 1),
        adults=2,
    )
    assert "airbnb.com.tr/s/" in url
    assert "checkin=2026-10-29" in url
    assert "adults=2" in url


def test_parse_listings():
    cards = parse_listings(FIXTURE.read_text(encoding="utf-8"))
    assert len(cards) == 3
    assert cards[0].name == "Modern Daire Kadikoy"
    assert cards[0].price_per_night == Decimal("2450.00")
    assert cards[2].price_per_night == Decimal("7200.00")
    assert all(c.provider == "airbnb" for c in cards)
