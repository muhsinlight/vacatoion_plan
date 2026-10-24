from decimal import Decimal

from app.jobs import job_targets
from app.models import ScrapeIngestRequest, ScrapeTarget
from app.settings import Settings


def test_targets_dry_run_collects_without_ingest(monkeypatch):
    target = ScrapeTarget(
        id=4,
        city_key="izmir",
        catalog_field="FOOD",
        source_url="https://prices.example/izmir",
        parser_type="JSON_MIN_MAX",
        label="İzmir yemek",
    )
    ingested = []

    class Api:
        def __enter__(self):
            return self

        def __exit__(self, *args):
            return False

        def list_targets(self):
            return [target]

        def ingest_target(self, request):
            ingested.append(request)

    monkeypatch.setattr("app.jobs.VacationApiClient", lambda settings: Api())
    monkeypatch.setattr(
        "app.jobs.scrape_targets.fetch_target",
        lambda *args: ScrapeIngestRequest(target_id=4, min=Decimal("10"), max=Decimal("20")),
    )
    monkeypatch.setattr("app.jobs.shared_budget", lambda settings: type("Budget", (), {"try_acquire": lambda self: True})())

    settings = Settings()
    settings.providers.targets.allowed_hosts = ["prices.example"]
    result = job_targets(settings, dry_run=True)

    assert result.collected == 1
    assert result.saved == 0
    assert result.errors == []
    assert ingested == []


def test_targets_job_saves_a_fetched_band(monkeypatch):
    target = ScrapeTarget(
        id=4,
        city_key="izmir",
        catalog_field="FOOD",
        source_url="https://prices.example/izmir",
        parser_type="JSON_MIN_MAX",
        label="İzmir yemek",
    )

    class Api:
        def __enter__(self):
            return self

        def __exit__(self, *args):
            return False

        def list_targets(self):
            return [target]

        def ingest_target(self, request):
            assert request.target_id == 4

    monkeypatch.setattr("app.jobs.VacationApiClient", lambda settings: Api())
    monkeypatch.setattr(
        "app.jobs.scrape_targets.fetch_target",
        lambda *args: ScrapeIngestRequest(target_id=4, min=Decimal("10"), max=Decimal("20")),
    )
    monkeypatch.setattr("app.jobs.shared_budget", lambda settings: type("Budget", (), {"try_acquire": lambda self: True})())

    settings = Settings()
    settings.providers.targets.allowed_hosts = ["prices.example"]
    result = job_targets(settings, dry_run=False)

    assert result.collected == 1
    assert result.saved == 1
    assert result.errors == []
