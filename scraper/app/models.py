"""API sözleşmesi. Örnek gövdeler ve enum listeleri kök contract/ altında; tests/test_contract.py ile Java tarafı aynı dosyaları okur."""

from datetime import date, datetime
from decimal import Decimal
from enum import StrEnum
from typing import Annotated

from pydantic import BaseModel, ConfigDict, Field, PlainSerializer
from pydantic.alias_generators import to_camel

Money = Annotated[Decimal, PlainSerializer(float, return_type=float, when_used="json")]


class BudgetCategory(StrEnum):
    TRANSPORT = "TRANSPORT"
    LODGING = "LODGING"
    FOOD = "FOOD"
    ACTIVITY = "ACTIVITY"
    LOCAL_TRANSPORT = "LOCAL_TRANSPORT"
    SHOPPING = "SHOPPING"
    OTHER = "OTHER"


class PriceScopeType(StrEnum):
    ROUTE = "ROUTE"
    DESTINATION = "DESTINATION"
    ACTIVITY_TEMPLATE = "ACTIVITY_TEMPLATE"
    NATIONAL_FUEL = "NATIONAL_FUEL"
    SCRAPE_SOURCE = "SCRAPE_SOURCE"


class PriceUnit(StrEnum):
    PER_PERSON = "PER_PERSON"
    PER_NIGHT = "PER_NIGHT"
    PER_TRIP = "PER_TRIP"
    PER_LITER = "PER_LITER"
    PER_DAY = "PER_DAY"


class PriceSourceType(StrEnum):
    OFFICIAL_API = "OFFICIAL_API"
    SCRAPE = "SCRAPE"
    ESTIMATE = "ESTIMATE"
    MANUAL = "MANUAL"
    IMPUTED = "IMPUTED"


class TransportMode(StrEnum):
    BUS = "BUS"
    TRAIN = "TRAIN"
    PLANE = "PLANE"
    CAR = "CAR"
    OTHER = "OTHER"


class FuelType(StrEnum):
    GASOLINE = "GASOLINE"
    DIESEL = "DIESEL"
    LPG = "LPG"


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class PriceObservation(CamelModel):
    category: BudgetCategory
    scope_type: PriceScopeType
    scope_key: str
    unit: PriceUnit
    min: Money
    typical: Money | None = None
    max: Money
    currency: str = "TRY"
    source_type: PriceSourceType
    provider: str | None = None
    source_url: str | None = None
    sample_size: int | None = None
    confidence: Money | None = None
    checked_at: datetime | None = None
    origin_key: str | None = None
    dest_key: str | None = None
    transport_mode: TransportMode | None = None
    week_bucket: date | None = None


class PriceObservationBatchResponse(CamelModel):
    saved: int
    route_quotes_updated: int
    observation_ids: list[int]


class ScrapeTarget(CamelModel):
    id: int
    city_key: str
    catalog_field: str
    activity_key: str | None = None
    source_url: str
    parser_type: str
    label: str


class ScrapeIngestRequest(CamelModel):
    target_id: int
    min: Money
    max: Money
    currency: str = "TRY"
    fetched_at: datetime | None = None


class BusLeg(CamelModel):
    origin: str
    dest: str
    travel_date: date


class BusRouteBatchRequest(CamelModel):
    legs: list[BusLeg]


class BusRouteBatchResponse(CamelModel):
    saved: int
    errors: list[str]


class LodgingTier(StrEnum):
    BUDGET = "BUDGET"
    MID = "MID"
    PREMIUM = "PREMIUM"


class AccommodationType(StrEnum):
    HOTEL = "HOTEL"
    PENSION = "PENSION"
    RENTAL = "RENTAL"
    CAMP = "CAMP"
    NONE = "NONE"


class LodgingSearchRequest(CamelModel):
    city: str
    check_in: date
    check_out: date
    adults: int = 2
    accommodation_type: AccommodationType = AccommodationType.HOTEL


class LodgingMoneyBand(CamelModel):
    min: Money
    typical: Money | None = None
    max: Money
    currency: str = "TRY"
    sample_size: int | None = None


class LodgingOfferDto(CamelModel):
    tier: LodgingTier
    provider: str
    name: str
    stars: int | None = None
    rating: Money | None = None
    board_type: str | None = None
    features: list[str] = Field(default_factory=list)
    price_per_night: Money
    total_price: Money | None = None
    url: str | None = None
    image_url: str | None = None


class LodgingSearchResponse(CamelModel):
    provider: str | None = None
    band: LodgingMoneyBand | None = None
    offers: list[LodgingOfferDto] = Field(default_factory=list)
    errors: list[str] = Field(default_factory=list)
    # Sayfa aralığı. Kartın kendi gecelik fiyatı değil.
    estimated: bool = False


class FoodSearchRequest(CamelModel):
    city: str


class FoodPlaceDto(CamelModel):
    name: str
    rating: Money | None = None
    review_count: int | None = None
    meal_min: Money | None = None
    meal_max: Money | None = None
    url: str | None = None
    michelin: bool = False


class FoodSearchResponse(CamelModel):
    city_key: str
    provider: str | None = None
    saved: int = 0
    band: LodgingMoneyBand | None = None
    places: list[FoodPlaceDto] = Field(default_factory=list)
    errors: list[str] = Field(default_factory=list)
