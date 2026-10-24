import json
from decimal import Decimal

import httpx
import pytest

from app.models import BudgetCategory, PriceObservation, PriceScopeType, PriceSourceType, PriceUnit
from app.settings import VacationApiSettings
from app.vacation_api import VacationApiClient

KEY = "s" * 32


def test_refuses_to_start_without_usable_key():
    with pytest.raises(RuntimeError):
        VacationApiClient(VacationApiSettings(scraper_key="dev-scraper-key"))


def test_post_observations_sends_key_and_camel_case_body():
    captured = {}

    def handler(request: httpx.Request) -> httpx.Response:
        captured["url"] = str(request.url)
        captured["key"] = request.headers["X-Scraper-Key"]
        captured["body"] = json.loads(request.content)
        return httpx.Response(200, json={"saved": 1, "routeQuotesUpdated": 0, "observationIds": [42]})

    settings = VacationApiSettings(base_url="http://api:8080/", scraper_key=KEY)
    observation = PriceObservation(
        category=BudgetCategory.TRANSPORT,
        scope_type=PriceScopeType.NATIONAL_FUEL,
        scope_key="DIESEL",
        unit=PriceUnit.PER_LITER,
        min=Decimal("96.50"),
        max=Decimal("97.70"),
        source_type=PriceSourceType.SCRAPE,
    )

    with VacationApiClient(settings, transport=httpx.MockTransport(handler)) as api:
        response = api.post_observations([observation])

    assert captured["url"] == "http://api:8080/api/v1/scraper/observations"
    assert captured["key"] == KEY
    assert captured["body"]["observations"][0]["scopeKey"] == "DIESEL"
    assert captured["body"]["observations"][0]["sourceType"] == "SCRAPE"
    assert response.observation_ids == [42]


def test_post_observations_retries_transient_failure():
    attempts = {"count": 0}

    def handler(request: httpx.Request) -> httpx.Response:
        attempts["count"] += 1
        if attempts["count"] == 1:
            return httpx.Response(503)
        return httpx.Response(200, json={"saved": 1, "routeQuotesUpdated": 0, "observationIds": [1]})

    settings = VacationApiSettings(base_url="http://api:8080/", scraper_key=KEY)
    observation = PriceObservation(
        category=BudgetCategory.OTHER,
        scope_type=PriceScopeType.NATIONAL_FUEL,
        scope_key="GASOLINE",
        unit=PriceUnit.PER_LITER,
        min=Decimal("40"),
        max=Decimal("45"),
        source_type=PriceSourceType.SCRAPE,
    )

    with VacationApiClient(settings, transport=httpx.MockTransport(handler)) as api:
        response = api.post_observations([observation])

    assert attempts["count"] == 2
    assert response.saved == 1
