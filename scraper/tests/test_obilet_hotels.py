from datetime import date
from decimal import Decimal
from pathlib import Path

import pytest

from app.lodging_tiers import select_tiers
from app.providers.obilet_hotels import parse_hotels, scrape_hotels, search_url
from app.settings import ObiletHotelSettings

FIXTURE = Path(__file__).parent / "fixtures" / "obilet_hotels_istanbul.html"


def test_search_url():
    url = search_url(
        ObiletHotelSettings(),
        "İstanbul",
        date(2026, 10, 29),
        date(2026, 11, 1),
        adults=2,
    )
    assert url.startswith("https://www.obilet.com/otel/istanbul-otelleri?")
    assert "checkin=2026-10-29" in url
    assert "checkout=2026-11-01" in url
    assert "adult=2" in url


def test_parse_hotels_from_page_model():
    result = parse_hotels(FIXTURE.read_text(encoding="utf-8"))
    assert result.min_price == Decimal("667.00")
    assert result.typical_price == Decimal("2852.71")
    assert len(result.cards) >= 3
    names = {c.name for c in result.cards}
    assert "Bricks Airport Hotel İstanbul" in names
    assert all(c.provider == "obilet" for c in result.cards)
    assert all(c.price_per_night == 0 for c in result.cards)


def test_scrape_refuses_cards_without_a_nightly_price(monkeypatch):
    html = FIXTURE.read_text(encoding="utf-8")

    class Response:
        text = html
        status_code = 200

        def raise_for_status(self):
            return None

    class Client:
        def __enter__(self):
            return self

        def __exit__(self, *args):
            return False

        def request(self, method, url, **kwargs):
            return Response()

    monkeypatch.setattr("app.providers.obilet_hotels.httpx.Client", lambda **kwargs: Client())
    scraped = scrape_hotels(
        ObiletHotelSettings(),
        "istanbul",
        date(2026, 10, 29),
        date(2026, 11, 1),
    )
    assert scraped.estimated
    assert [card.name for card in scraped.cards] == ["Obilet şehir aralığı"]
    assert scraped.band_min == Decimal("667.00")
    assert scraped.band_typical == Decimal("2852.71")
    assert scraped.cards[0].price_per_night == Decimal("2852.71")
    assert any("şehir aralığı" in error for error in scraped.errors)


def test_offer_payload_uses_nightly_price():
    from app.providers.obilet_hotels import cards_from_offer_payload

    cards = cards_from_offer_payload(
        {"data": {"searchHotel": [{
            "id": 107031,
            "name": "Adana Altın Koza Hotel",
            "stars": 4,
            "averageScore": 9.2,
            "offers": [{
                "night": 2,
                "price": {"amount": 4140.0, "currency": "TRY"},
                "boardItem": {"name": "Oda Kahvaltı"},
            }],
        }]}},
        city_slug_value="adana",
        origin_id="217",
        external_id="61773",
        dates="20261023-20261025",
        pax="1ad",
    )
    assert len(cards) == 1
    assert cards[0].price_per_night == Decimal("2070.00")
    assert cards[0].url.startswith("https://www.obilet.com/oteller/adana-altin-koza-hotel-107031_")


def test_hotel_origin_reads_external_id():
    from app.providers.obilet_hotels import _hotel_origin

    html = '"origin":{"id":"217","hotelId":0,"externalId":"61773","text":"adana"}'
    assert _hotel_origin(html) == ("217", "61773")


def test_unpriced_cards_do_not_become_offers():
    result = parse_hotels(FIXTURE.read_text(encoding="utf-8"))
    with pytest.raises(ValueError):
        select_tiers(result.cards)
