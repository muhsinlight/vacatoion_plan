"""Konaklama arama orkestrasyonu: Obilet otel/pansiyon, Airbnb kiralık.

Canlı kartlar bu süreçte katalog gözlemi yazmaz. API aramayı çağırır ve
`lodging_offers` tablosuna kendi tarafında işler. `LODGING_PER_NIGHT` ingest kapalıdır.
"""

from __future__ import annotations

from decimal import Decimal

from app.fetch_budget import LIMIT_ERROR, shared_budget
from app.lodging_tiers import LodgingCard, select_tiers, tier_assignments
from app.models import (
    AccommodationType,
    LodgingMoneyBand,
    LodgingOfferDto,
    LodgingSearchRequest,
    LodgingSearchResponse,
    LodgingTier,
)
from app.providers import airbnb_rentals, obilet_hotels
from app.settings import Settings


def search_lodging(settings: Settings, request: LodgingSearchRequest) -> LodgingSearchResponse:
    if request.check_out <= request.check_in:
        return LodgingSearchResponse(errors=["checkOut checkIn'den sonra olmalı"])

    adults = max(1, request.adults)
    acc = request.accommodation_type
    live_types = (AccommodationType.HOTEL, AccommodationType.PENSION, AccommodationType.RENTAL)
    if acc in live_types and not shared_budget(settings).try_acquire():
        return LodgingSearchResponse(errors=[f"{request.city}: {LIMIT_ERROR}"])

    estimated = False
    band_override = None
    if acc in (AccommodationType.HOTEL, AccommodationType.PENSION):
        scraped = obilet_hotels.scrape_hotels(
            settings.providers.obilet_hotel,
            request.city,
            request.check_in,
            request.check_out,
            adults,
            accommodation_hint=acc.value,
        )
        cards, errors = scraped.cards, scraped.errors
        estimated = scraped.estimated
        if estimated and scraped.band_min is not None and scraped.band_max is not None:
            band_override = LodgingMoneyBand(
                min=scraped.band_min,
                typical=scraped.band_typical,
                max=scraped.band_max,
                sample_size=None,
            )
    elif acc == AccommodationType.RENTAL:
        cards, errors = airbnb_rentals.scrape_rentals(
            settings.providers.airbnb,
            request.city,
            request.check_in,
            request.check_out,
            adults,
        )
    else:
        return LodgingSearchResponse(
            errors=[f"{acc.value} için canlı konaklama kaynağı yok"],
        )

    provider = cards[0].provider if cards else None
    if not cards:
        return LodgingSearchResponse(provider=provider, errors=errors)

    nights = (request.check_out - request.check_in).days
    if estimated:
        card = cards[0]
        total = (card.price_per_night * nights).quantize(Decimal("0.01")) if nights > 0 else None
        offer = LodgingOfferDto(
            tier=LodgingTier.MID,
            provider=card.provider,
            name=card.name,
            price_per_night=card.price_per_night,
            total_price=total,
            url=card.url,
        )
        return LodgingSearchResponse(
            provider=provider,
            band=band_override,
            offers=[offer],
            errors=errors,
            estimated=True,
        )

    offers = _to_offers(cards, nights)
    band = _band_from_cards(cards)
    return LodgingSearchResponse(provider=provider, band=band, offers=offers, errors=errors)


def _to_offers(cards: list[LodgingCard], nights: int) -> list[LodgingOfferDto]:
    # Aynı otel adı iki kademeye yazılmaz.
    try:
        ordered = [(LodgingTier(label), card) for label, card in tier_assignments(cards)]
    except ValueError:
        ordered = [(LodgingTier.MID, c) for c in cards[:3]]

    offers: list[LodgingOfferDto] = []
    for tier, card in ordered:
        total = None
        if nights > 0:
            total = (card.price_per_night * nights).quantize(Decimal("0.01"))
        offers.append(LodgingOfferDto(
            tier=tier,
            provider=card.provider,
            name=card.name,
            stars=card.stars,
            rating=card.rating,
            board_type=card.board_type,
            features=list(card.features),
            price_per_night=card.price_per_night,
            total_price=total,
            url=card.url,
            image_url=card.image_url,
        ))
    return offers


def _band_from_cards(cards: list[LodgingCard]) -> LodgingMoneyBand:
    tiers = select_tiers(cards)
    return LodgingMoneyBand(
        min=tiers.band.min,
        typical=tiers.band.typical,
        max=tiers.band.max,
        sample_size=tiers.band.sample_size,
    )
