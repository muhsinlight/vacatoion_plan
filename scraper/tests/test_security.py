from datetime import date

import pytest
from fastapi.testclient import TestClient

from app import main
from app.security import key_matches
from app.settings import Settings

KEY = "t" * 32


@pytest.fixture
def client(monkeypatch):
    settings = Settings()
    settings.server.trigger_key = KEY
    monkeypatch.setattr("app.security.get_settings", lambda: settings)
    monkeypatch.setattr(main, "get_settings", lambda: settings)
    monkeypatch.setattr(main, "search_lodging", lambda _s, _b: main.LodgingSearchResponse())
    main.app.state.scheduler = None
    return TestClient(main.app), settings


def lodging_body() -> dict:
    return {"city": "isparta", "checkIn": date(2026, 10, 29).isoformat(),
            "checkOut": date(2026, 11, 1).isoformat(), "accommodationType": "HOTEL"}


def test_key_matches_requires_long_key():
    assert key_matches(KEY, KEY)
    assert not key_matches("dev-scraper-key", "dev-scraper-key")
    assert not key_matches(KEY, None)
    assert not key_matches(KEY, KEY[:-1] + "x")


def test_trigger_endpoints_reject_missing_and_wrong_key(client):
    http, _ = client
    assert http.post("/lodging/search", json=lodging_body()).status_code == 401
    assert http.post("/lodging/search", json=lodging_body(), headers={"X-Scraper-Key": "x" * 32}).status_code == 401
    assert http.post("/jobs/fuel", headers={"X-Scraper-Key": "wrong"}).status_code == 401
    assert http.post("/routes/bus", json={"legs": []}).status_code == 401


def test_trigger_endpoints_accept_matching_key(client):
    http, _ = client
    response = http.post("/lodging/search", json=lodging_body(), headers={"X-Scraper-Key": KEY})
    assert response.status_code == 200


def test_trigger_endpoints_closed_when_key_not_configured(client):
    http, settings = client
    settings.server.trigger_key = ""
    assert http.post("/lodging/search", json=lodging_body(), headers={"X-Scraper-Key": ""}).status_code == 503


def test_ingest_key_does_not_open_trigger_endpoints(client):
    http, settings = client
    settings.vacation_api.scraper_key = "i" * 32
    response = http.post("/lodging/search", json=lodging_body(), headers={"X-Scraper-Key": "i" * 32})
    assert response.status_code == 401
