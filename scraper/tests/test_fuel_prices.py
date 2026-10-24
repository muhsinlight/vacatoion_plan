from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path

from scrapling.parser import Selector

from app.models import FuelType
from app.providers.fuel_prices import build_observations, parse_city_prices

FIXTURE = Path(__file__).parent / "fixtures" / "petrol_ofisi.html"


def test_parse_city_prices_reads_with_tax_columns():
    prices = parse_city_prices(Selector(FIXTURE.read_text(encoding="utf-8")))

    assert prices[FuelType.GASOLINE] == [Decimal("83.80"), Decimal("83.60"), Decimal("84.80")]
    assert prices[FuelType.DIESEL] == [Decimal("96.60"), Decimal("96.50"), Decimal("97.70")]
    assert prices[FuelType.LPG] == [Decimal("41.20"), Decimal("40.60"), Decimal("41.29")]


def test_build_observations_matches_ingest_contract():
    prices = parse_city_prices(Selector(FIXTURE.read_text(encoding="utf-8")))
    observations = build_observations(prices, "https://po", datetime(2026, 10, 14, tzinfo=timezone.utc))

    by_key = {o.scope_key: o.model_dump(mode="json", by_alias=True, exclude_none=True) for o in observations}
    assert set(by_key) == {"GASOLINE", "DIESEL", "LPG"}
    gasoline = by_key["GASOLINE"]
    assert gasoline["scopeType"] == "NATIONAL_FUEL"
    assert gasoline["unit"] == "PER_LITER"
    assert gasoline["provider"] == "petrol-ofisi"
    assert gasoline["sourceUrl"] == "https://po"
    assert (gasoline["min"], gasoline["typical"], gasoline["max"]) == (83.6, 83.8, 84.8)
    assert "transportMode" not in gasoline
