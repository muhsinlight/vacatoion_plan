import json
from decimal import Decimal
from pathlib import Path

from app.providers.google_places import (
    daily_band,
    merge_restaurant_lists,
    parse_money,
    province_key,
    Restaurant,
    select_restaurants,
)

FIXTURE = Path(__file__).parent / "fixtures" / "google_places_restaurants.json"


def test_select_restaurants_ranks_by_stars_then_reviews():
    payload = json.loads(FIXTURE.read_text(encoding="utf-8"))
    places = select_restaurants(payload, min_rating=4.0, min_reviews=20, limit=8)

    assert [place.name for place in places] == ["Fiyatsız", "Lokanta", "Dolarlı", "Kebapçı"]
    assert places[0].meal_min is None
    assert places[0].meal_max is None
    assert places[1].rating == Decimal("4.8")
    assert places[1].review_count == 200
    assert places[3].meal_min == Decimal("200.50")
    assert places[3].url.startswith("https://maps.google.com/")


def test_daily_band_scales_one_meal_to_a_day():
    places = select_restaurants(
        json.loads(FIXTURE.read_text(encoding="utf-8")),
        min_rating=4.0,
        min_reviews=20,
        limit=8,
    )
    band = daily_band(places, Decimal("2.5"))

    assert band.min == Decimal("501.25")
    assert band.max == Decimal("1750.00")
    assert band.min <= band.typical <= band.max
    assert band.sample_size == 2


def test_parse_money_reads_nanos():
    code, amount = parse_money({"currencyCode": "TRY", "units": "10", "nanos": 250000000})
    assert code == "TRY"
    assert amount == Decimal("10.25")


def test_province_key_follows_alias():
    assert province_key("Eğirdir") == "isparta"
    assert province_key("Isparta") == "isparta"


def test_merge_restaurant_lists_prioritizes_michelin():
    general = [
        Restaurant("A", Decimal("4.9"), 500),
        Restaurant("B", Decimal("4.7"), 300),
    ]
    michelin = [
        Restaurant("C Michelin", Decimal("4.2"), 40, michelin=True),
    ]
    merged = merge_restaurant_lists([general, michelin], limit=3)
    assert merged[0].michelin is True
    assert merged[0].name == "C Michelin"
