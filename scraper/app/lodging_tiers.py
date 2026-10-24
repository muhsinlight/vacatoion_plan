"""Konaklama kartlarından fiyat bandı ve BUDGET / MID / PREMIUM kademeleri."""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal

from app.bands import Band, band_from_prices


@dataclass(frozen=True)
class LodgingCard:
    name: str
    price_per_night: Decimal
    provider: str
    stars: int | None = None
    rating: Decimal | None = None
    board_type: str | None = None
    features: tuple[str, ...] = ()
    url: str | None = None
    image_url: str | None = None
    total_price: Decimal | None = None


@dataclass(frozen=True)
class TieredLodging:
    band: Band
    budget: LodgingCard
    mid: LodgingCard
    premium: LodgingCard


def unique_priced(cards: list[LodgingCard]) -> list[LodgingCard]:
    """Aynı otel adını bir kez tutar; en düşük gecelik fiyat kalır."""
    priced = sorted(
        (c for c in cards if c.price_per_night > 0),
        key=lambda c: (c.price_per_night, c.name.casefold()),
    )
    unique: list[LodgingCard] = []
    seen: set[str] = set()
    for card in priced:
        key = card.name.casefold()
        if key in seen:
            continue
        seen.add(key)
        unique.append(card)
    return unique


def select_tiers(cards: list[LodgingCard]) -> TieredLodging:
    """Farklı otellerden en ucuz, ortanca ve en pahalıyı seçer."""
    priced = unique_priced(cards)
    if not priced:
        raise ValueError("Fiyatlı konaklama kartı yok")

    band = band_from_prices([c.price_per_night for c in priced])
    budget = priced[0]
    premium = priced[-1]
    mid = priced[len(priced) // 2]
    return TieredLodging(band=band, budget=budget, mid=mid, premium=premium)


def tier_assignments(cards: list[LodgingCard]) -> list[tuple[str, LodgingCard]]:
    """Her otel adını tek kademeye yazar. Tek otel MID olur."""
    tiers = select_tiers(cards)
    rows: list[tuple[str, LodgingCard]] = []
    seen: set[str] = set()
    for label, card in (
        ("BUDGET", tiers.budget),
        ("MID", tiers.mid),
        ("PREMIUM", tiers.premium),
    ):
        key = card.name.casefold()
        if key in seen:
            continue
        seen.add(key)
        rows.append((label, card))
    if len(rows) == 1 and rows[0][0] != "MID":
        return [("MID", rows[0][1])]
    return rows


def assign_band_prices(
        cards: list[LodgingCard],
        min_price: Decimal,
        typical_price: Decimal,
        max_price: Decimal | None = None,
) -> list[LodgingCard]:
    """Kartlarda fiyat yoksa yıldız/puana göre min-typical-max bandından atar."""
    if not cards:
        return []
    hi = max_price if max_price and max_price >= typical_price else (typical_price * Decimal("1.75")).quantize(Decimal("0.01"))
    lo = min_price if min_price > 0 else typical_price
    if lo > typical_price:
        lo, typical_price = typical_price, lo

    ranked = sorted(
        cards,
        key=lambda c: (c.stars or 0, c.rating or Decimal("0"), c.name),
    )
    n = len(ranked)
    out: list[LodgingCard] = []
    for index, card in enumerate(ranked):
        if n == 1:
            price = typical_price
        elif index == 0:
            price = lo
        elif index == n - 1:
            price = hi
        else:
            # ara kartlar low-typical-high arasında doğrusal
            ratio = Decimal(index) / Decimal(n - 1)
            price = (lo + (hi - lo) * ratio).quantize(Decimal("0.01"))
        out.append(LodgingCard(
            name=card.name,
            price_per_night=price,
            provider=card.provider,
            stars=card.stars,
            rating=card.rating,
            board_type=card.board_type,
            features=card.features,
            url=card.url,
            image_url=card.image_url,
            total_price=card.total_price,
        ))
    return out
