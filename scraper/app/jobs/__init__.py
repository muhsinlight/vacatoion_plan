import json
import logging
from collections.abc import Callable
from dataclasses import asdict, dataclass, field

import httpx

from app.fetch_budget import LIMIT_ERROR, shared_budget
from app.models import PriceObservation
from app.providers import fuel_prices, obilet_bus, scrape_targets
from app.settings import Settings
from app.vacation_api import VacationApiClient

log = logging.getLogger(__name__)


@dataclass
class JobResult:
    job: str
    collected: int = 0
    saved: int = 0
    route_quotes_updated: int = 0
    dry_run: bool = False
    errors: list[str] = field(default_factory=list)

    def to_dict(self) -> dict:
        return asdict(self)


def _submit(settings: Settings, result: JobResult, observations: list[PriceObservation]) -> JobResult:
    result.collected = len(observations)
    if not observations:
        return result
    if result.dry_run:
        for o in observations:
            log.info("[dry-run] %s", json.dumps(o.model_dump(mode="json", by_alias=True, exclude_none=True), ensure_ascii=False))
        return result
    with VacationApiClient(settings.vacation_api) as api:
        response = api.post_observations(observations)
    result.saved = response.saved
    result.route_quotes_updated = response.route_quotes_updated
    return result


def job_bus(settings: Settings, dry_run: bool = False) -> JobResult:
    result = JobResult(job="bus", dry_run=dry_run)
    if not settings.route_targets:
        result.errors.append("route_targets boş")
        return result
    observations, result.errors = obilet_bus.scrape_routes(
        settings.providers.obilet, settings.route_targets, acquire=shared_budget(settings).try_acquire)
    return _submit(settings, result, observations)


def job_fuel(settings: Settings, dry_run: bool = False) -> JobResult:
    result = JobResult(job="fuel", dry_run=dry_run)
    if not shared_budget(settings).try_acquire():
        result.errors.append(LIMIT_ERROR)
        return result
    try:
        observations = fuel_prices.scrape_fuel(settings.providers.fuel.url)
    except Exception as ex:
        log.exception("Yakıt işi başarısız")
        result.errors.append(str(ex))
        return result
    return _submit(settings, result, observations)


def job_food(settings: Settings, dry_run: bool = False) -> JobResult:
    """Toplu il turu yok. Restoran listesi önizlemede, istenen il için çekilir; katalog gözlemi yazılmaz."""
    return JobResult(job="food", dry_run=dry_run)


def job_targets(settings: Settings, dry_run: bool = False) -> JobResult:
    result = JobResult(job="targets", dry_run=dry_run)
    budget = shared_budget(settings)
    allowed_hosts = settings.providers.targets.allowed_hosts
    if not allowed_hosts:
        result.errors.append("allowed_hosts boş; targets işi atlandı")
        return result
    with VacationApiClient(settings.vacation_api) as api, httpx.Client(timeout=30, follow_redirects=False) as http:
        for target in api.list_targets():
            if not budget.try_acquire():
                result.errors.append(f"target {target.id} ({target.label}): {LIMIT_ERROR}")
                break
            try:
                request = scrape_targets.fetch_target(target, http, allowed_hosts)
                result.collected += 1
                if dry_run:
                    log.info("[dry-run] %s", request.model_dump_json(by_alias=True, exclude_none=True))
                    continue
                api.ingest_target(request)
                result.saved += 1
            except Exception as ex:
                log.exception("Hedef %s başarısız", target.id)
                result.errors.append(f"target {target.id} ({target.label}): {ex}")
    return result


JOBS: dict[str, Callable[[Settings, bool], JobResult]] = {
    "bus": job_bus,
    "fuel": job_fuel,
    "food": job_food,
    "targets": job_targets,
}
