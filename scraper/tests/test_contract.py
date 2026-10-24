"""Kök contract/ örnekleri; aynı dosyaları api/.../contract/ScraperContractTest.java da okur."""

import json
from pathlib import Path

import pytest
from pydantic import BaseModel

from app import models
from app.models import (
    BusRouteBatchRequest,
    BusRouteBatchResponse,
    LodgingSearchRequest,
    LodgingSearchResponse,
    PriceObservation,
    PriceObservationBatchResponse,
    ScrapeIngestRequest,
    ScrapeTarget,
)
from app.providers.obilet_locations import city_slug
from app.providers.scrape_targets import PARSERS

CONTRACT = Path(__file__).resolve().parents[2] / "contract"


class PriceObservationBatch(BaseModel):
    observations: list[PriceObservation]


def load(name: str):
    return json.loads((CONTRACT / name).read_text(encoding="utf-8"))


def paths(node, prefix: str = "") -> set[str]:
    result: set[str] = set()
    if isinstance(node, dict):
        for key, value in node.items():
            path = f"{prefix}/{key}"
            result.add(path)
            result |= paths(value, path)
    elif isinstance(node, list):
        for item in node:
            result |= paths(item, prefix + "[]")
    return result


@pytest.mark.parametrize(("name", "model"), [
    ("price_observation_batch.json", PriceObservationBatch),
    ("price_observation_batch_response.json", PriceObservationBatchResponse),
    ("bus_route_batch_request.json", BusRouteBatchRequest),
    ("bus_route_batch_response.json", BusRouteBatchResponse),
    ("lodging_search_request.json", LodgingSearchRequest),
    ("lodging_search_response.json", LodgingSearchResponse),
    ("scrape_target.json", ScrapeTarget),
    ("scrape_ingest_request.json", ScrapeIngestRequest),
])
def test_model_round_trip_keeps_every_field(name, model):
    fixture = load(name)
    dumped = model.model_validate(fixture).model_dump(mode="json", by_alias=True)
    assert paths(dumped) == paths(fixture)


def test_enums_match_contract():
    for name, values in load("enums.json").items():
        if name == "ScrapeParserType":
            assert set(PARSERS) == set(values)
            continue
        assert {member.value for member in getattr(models, name)} == set(values), name


def test_api_city_keys_are_stable_slugs():
    for row in load("city_keys.json"):
        assert city_slug(row["key"]) == row["key"]
