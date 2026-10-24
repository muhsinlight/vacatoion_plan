import logging
import os
from contextlib import asynccontextmanager
from datetime import date

from fastapi import Depends, FastAPI, HTTPException

from app.fetch_budget import shared_budget
from app.food_search import search_food
from app.jobs import JOBS
from app.jobs.scheduler import build_scheduler
from app.lodging_search import search_lodging
from app.models import (
    BusRouteBatchRequest,
    BusRouteBatchResponse,
    FoodSearchRequest,
    FoodSearchResponse,
    LodgingSearchRequest,
    LodgingSearchResponse,
)
from app.providers import obilet_bus
from app.providers.obilet_locations import resolve_route
from app.security import is_usable, require_trigger_key
from app.settings import RouteTarget, get_settings
from app.vacation_api import VacationApiClient

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    if not is_usable(settings.server.trigger_key):
        log.warning("SCRAPER_TRIGGER_KEY yok; /routes/bus, /lodging/search, /food/search ve /jobs 503 döner")
    if not is_usable(settings.vacation_api.scraper_key):
        log.warning("SCRAPER_API_KEY yok; zamanlanmış işler API'ye yazamaz")
    scheduler = None
    if os.environ.get("SCRAPER_SCHEDULER_ENABLED", "true").lower() == "true":
        scheduler = build_scheduler(settings)
        scheduler.start()
    app.state.scheduler = scheduler
    yield
    if scheduler:
        scheduler.shutdown(wait=False)


app = FastAPI(title="Vacation Plan Scraper", lifespan=lifespan, docs_url=None, redoc_url=None, openapi_url=None)


@app.get("/health")
def health() -> dict:
    scheduler = app.state.scheduler
    return {"status": "UP", "scheduler": scheduler is not None}


@app.post("/food/search", dependencies=[Depends(require_trigger_key)])
def food_search(body: FoodSearchRequest) -> FoodSearchResponse:
    """Şehir merkezindeki OpenStreetMap restoranları. Günlük tutar katalog bandında kalır."""
    if not body.city or not body.city.strip():
        raise HTTPException(status_code=400, detail="city gerekli")
    return search_food(get_settings(), body)


@app.post("/lodging/search", dependencies=[Depends(require_trigger_key)])
def lodging_search(body: LodgingSearchRequest) -> LodgingSearchResponse:
    """Şehir + tarihler için Obilet otel/pansiyon veya Airbnb kiralık fiyat bandı ve 3 kademe."""
    if not body.city or not body.city.strip():
        raise HTTPException(status_code=400, detail="city gerekli")
    return search_lodging(get_settings(), body)


@app.post("/routes/bus", dependencies=[Depends(require_trigger_key)])
def scrape_bus_routes(body: BusRouteBatchRequest) -> BusRouteBatchResponse:
    """İstenen gidiş/dönüş tarihlerinde Obilet fiyat bandını çekip API'ye yazar."""
    settings = get_settings()
    if not body.legs:
        raise HTTPException(status_code=400, detail="En az bir bacak gerekli")

    legs: list[tuple[RouteTarget, date]] = []
    errors: list[str] = []
    for leg in body.legs:
        try:
            legs.append((resolve_route(leg.origin, leg.dest), leg.travel_date))
        except Exception as ex:
            errors.append(f"{leg.origin}->{leg.dest}: {ex}")
    observations, scrape_errors = (
        obilet_bus.scrape_legs(settings.providers.obilet, legs, acquire=shared_budget(settings).try_acquire)
        if legs else ([], [])
    )
    errors.extend(scrape_errors)
    saved = 0
    if observations:
        try:
            with VacationApiClient(settings.vacation_api) as api:
                saved = api.post_observations(observations).saved
        except Exception as ex:
            log.exception("Gözlemler API'ye yazılamadı")
            errors.append(f"API'ye yazılamadı: {ex}")
    return BusRouteBatchResponse(saved=saved, errors=errors)


@app.post("/jobs/{name}", dependencies=[Depends(require_trigger_key)])
def run_job(name: str, dry_run: bool = False) -> dict:
    if name not in JOBS:
        raise HTTPException(status_code=404, detail=f"Bilinmeyen job: {name}")
    return JOBS[name](get_settings(), dry_run).to_dict()
