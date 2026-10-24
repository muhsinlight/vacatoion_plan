from datetime import date
from decimal import Decimal

import pytest

from app.bands import band_from_prices, parse_tr_amount, week_bucket


@pytest.mark.parametrize(("text", "expected"), [
    ("1.020,00 TL", Decimal("1020.00")),
    ("1.020", Decimal("1020")),
    ("971,50", Decimal("971.50")),
    ("83.80", Decimal("83.80")),
    ("700", Decimal("700")),
])
def test_parse_tr_amount(text, expected):
    assert parse_tr_amount(text) == expected


def test_parse_tr_amount_rejects_empty():
    with pytest.raises(ValueError):
        parse_tr_amount("TL")


def test_band_uses_median_as_typical():
    band = band_from_prices([Decimal("910"), Decimal("700"), Decimal("1020"), Decimal("1000")])
    assert (band.min, band.typical, band.max) == (Decimal("700.00"), Decimal("955.00"), Decimal("1020.00"))
    assert band.sample_size == 4
    assert band.confidence == Decimal("0.70")


def test_week_bucket_is_iso_monday():
    assert week_bucket(date(2026, 10, 28)) == date(2026, 10, 26)
    assert week_bucket(date(2026, 10, 26)) == date(2026, 10, 26)
