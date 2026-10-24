from decimal import Decimal

from app.lodging_tiers import LodgingCard, assign_band_prices, select_tiers, tier_assignments


def _card(name: str, price: str, stars: int = 3) -> LodgingCard:
    return LodgingCard(
        name=name,
        price_per_night=Decimal(price),
        provider="obilet",
        stars=stars,
    )


def test_select_tiers_picks_min_mid_max():
    cards = [
        _card("A", "1000"),
        _card("B", "2000"),
        _card("C", "3000"),
        _card("D", "4000"),
        _card("E", "5000"),
    ]
    tiers = select_tiers(cards)
    assert tiers.budget.name == "A"
    assert tiers.premium.name == "E"
    assert tiers.mid.name == "C"
    assert tiers.band.min == Decimal("1000.00")
    assert tiers.band.max == Decimal("5000.00")


def test_tier_assignments_drop_duplicate_hotel_names():
    cards = [
        _card("Cimenler Hotel", "2824"),
        _card("Ramada By Wyndham", "4317"),
        _card("Ramada By Wyndham", "4317"),
    ]
    rows = tier_assignments(cards)
    assert [label for label, _card in rows] == ["BUDGET", "MID"]
    assert [card.name for _label, card in rows] == ["Cimenler Hotel", "Ramada By Wyndham"]


def test_assign_band_prices_by_stars():
    cards = [
        LodgingCard(name="Low", price_per_night=Decimal("0"), provider="obilet", stars=2),
        LodgingCard(name="Mid", price_per_night=Decimal("0"), provider="obilet", stars=3),
        LodgingCard(name="High", price_per_night=Decimal("0"), provider="obilet", stars=5),
    ]
    priced = assign_band_prices(cards, Decimal("667"), Decimal("2852.71"))
    by_name = {c.name: c.price_per_night for c in priced}
    assert by_name["Low"] == Decimal("667.00")
    assert by_name["High"] > by_name["Mid"] > by_name["Low"]
