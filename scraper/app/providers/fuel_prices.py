import time
from datetime import datetime, timezone
from decimal import Decimal

from scrapling.fetchers import Fetcher
from scrapling.parser import Selector

from app.bands import band_from_prices, parse_tr_amount
from app.models import BudgetCategory, FuelType, PriceObservation, PriceScopeType, PriceSourceType, PriceUnit

# Akaryakıt tablosu: Şehir | Kurşunsuz 95 | Diesel | Gazyağı | Kalorifer Yakıtı | Fuel Oil | Otogaz
COLUMN_BY_FUEL = {
    FuelType.GASOLINE: 1,
    FuelType.DIESEL: 2,
    FuelType.LPG: 6,
}


def parse_city_prices(page: Selector) -> dict[FuelType, list[Decimal]]:
    prices: dict[FuelType, list[Decimal]] = {fuel: [] for fuel in COLUMN_BY_FUEL}
    for row in page.css("table.table-prices tr.price-row"):
        cells = row.css("td")
        for fuel, column in COLUMN_BY_FUEL.items():
            if column >= len(cells):
                continue
            text = cells[column].css("span.with-tax::text").get()
            if text and text.strip():
                prices[fuel].append(parse_tr_amount(text))
    return prices


def build_observations(
        prices: dict[FuelType, list[Decimal]],
        _source_url: str,
        checked_at: datetime,
) -> list[PriceObservation]:
    observations = []
    for fuel, values in prices.items():
        if not values:
            continue
        band = band_from_prices(values)
        observations.append(PriceObservation(
            category=BudgetCategory.TRANSPORT,
            scope_type=PriceScopeType.NATIONAL_FUEL,
            scope_key=fuel.value,
            unit=PriceUnit.PER_LITER,
            min=band.min,
            typical=band.typical,
            max=band.max,
            source_type=PriceSourceType.SCRAPE,
            provider="petrol-ofisi",
            source_url=_source_url,
            sample_size=band.sample_size,
            confidence=band.confidence,
            checked_at=checked_at,
        ))
    return observations


def scrape_fuel(url: str) -> list[PriceObservation]:
    page = None
    for attempt in range(3):
        page = Fetcher.get(url, timeout=30)
        if page.status in {408, 429, 500, 502, 503, 504} and attempt < 2:
            time.sleep(0.5 * (2**attempt))
            continue
        break
    if page is None or page.status != 200:
        raise RuntimeError(f"Akaryakıt sayfası HTTP {page.status if page is not None else 'yok'}")
    observations = build_observations(parse_city_prices(page), url, datetime.now(timezone.utc))
    if not observations:
        raise RuntimeError("Akaryakıt tablosu ayrıştırılamadı")
    return observations
