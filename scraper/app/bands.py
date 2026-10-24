import re
from dataclasses import dataclass
from datetime import date, timedelta
from decimal import ROUND_HALF_UP, Decimal
from statistics import median

CENTS = Decimal("0.01")


@dataclass(frozen=True)
class Band:
    min: Decimal
    typical: Decimal
    max: Decimal
    sample_size: int
    confidence: Decimal


def band_from_prices(prices: list[Decimal]) -> Band:
    if not prices:
        raise ValueError("Fiyat listesi boş")
    values = sorted(prices)
    return Band(
        min=values[0].quantize(CENTS),
        typical=Decimal(median(values)).quantize(CENTS, rounding=ROUND_HALF_UP),
        max=values[-1].quantize(CENTS),
        sample_size=len(values),
        confidence=confidence_for(len(values)),
    )


def confidence_for(sample_size: int) -> Decimal:
    if sample_size >= 5:
        return Decimal("0.90")
    if sample_size >= 2:
        return Decimal("0.70")
    return Decimal("0.50")


def parse_tr_amount(text: str) -> Decimal:
    """'1.020,00 TL' / '1.020' / '83.80' -> Decimal. Virgül varsa Türkçe biçim kabul edilir."""
    cleaned = re.sub(r"[^\d.,]", "", text)
    if not cleaned:
        raise ValueError(f"Tutar bulunamadı: {text!r}")
    if "," in cleaned:
        cleaned = cleaned.replace(".", "").replace(",", ".")
    elif re.fullmatch(r"\d{1,3}(\.\d{3})+", cleaned):
        cleaned = cleaned.replace(".", "")
    return Decimal(cleaned)


def week_bucket(day: date) -> date:
    return day - timedelta(days=day.weekday())
