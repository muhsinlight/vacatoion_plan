import os
from functools import lru_cache
from pathlib import Path

import yaml
from pydantic import BaseModel, Field

SCRAPER_DIR = Path(__file__).resolve().parent.parent
DEFAULT_CONFIG_PATH = SCRAPER_DIR / "config" / "settings.yaml"
# Önce gelen kazanır; işletim sistemi değişkeni hepsini ezer.
ENV_FILES = (SCRAPER_DIR / ".env", SCRAPER_DIR.parent / ".env")


class VacationApiSettings(BaseModel):
    base_url: str = "http://127.0.0.1:8080"
    # Scraper → API (/api/v1/scraper/**). Boşsa API'ye yazılmaz.
    scraper_key: str = ""
    timeout_seconds: float = 30


class ServerSettings(BaseModel):
    # API → scraper (/routes/bus, /lodging/search, /jobs). Boşsa bu uçlar 503 döner.
    trigger_key: str = ""


class LimitSettings(BaseModel):
    # Zamanlanmış işler ve API tetiklemeleri aynı sayacı kullanır. 0 = sınırsız.
    site_fetches_per_hour: int = 60


class SchedulerSettings(BaseModel):
    timezone: str = "Europe/Istanbul"
    jobs: dict[str, str] = Field(default_factory=dict)


class RouteTarget(BaseModel):
    origin: str
    dest: str
    obilet_origin_id: int
    obilet_dest_id: int


class ObiletSettings(BaseModel):
    search_url: str = "https://www.obilet.com/seferler/{origin_id}-{dest_id}/{date}"
    sample_days_ahead: int = 14
    headless: bool = True
    timeout_ms: int = 60000
    delay_between_routes_seconds: float = 5


class ObiletHotelSettings(BaseModel):
    search_url: str = "https://www.obilet.com/otel/{city_slug}"
    headless: bool = True
    timeout_ms: int = 60000


class AirbnbSettings(BaseModel):
    search_url: str = "https://www.airbnb.com.tr/s/{city_slug}/homes"
    headless: bool = True
    timeout_ms: int = 45000


class FuelSettings(BaseModel):
    url: str = "https://www.petrolofisi.com.tr/akaryakit-fiyatlari"


class OsmSettings(BaseModel):
    nominatim_url: str = "https://nominatim.openstreetmap.org/search"
    overpass_url: str = "https://overpass.openstreetmap.fr/api/interpreter"
    overpass_fallbacks: list[str] = Field(default_factory=lambda: [
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass-api.de/api/interpreter",
    ])
    user_agent: str = "VacationPlan/1.0 (local food list)"
    radius_m: int = 6000
    result_limit: int = 8
    timeout_seconds: float = 40

    def overpass_endpoints(self) -> list[str]:
        urls = [self.overpass_url]
        for extra in self.overpass_fallbacks:
            if extra not in urls:
                urls.append(extra)
        return urls


class GooglePlacesSettings(BaseModel):
    search_url: str = "https://places.googleapis.com/v1/places:searchText"
    # Anahtar settings.yaml'da durmaz. GOOGLE_PLACES_API_KEY ortamdan gelir.
    api_key: str = ""
    min_rating: float = 4.0
    min_reviews: int = 20
    result_limit: int = 8
    page_size: int = 20
    include_michelin_search: bool = True
    michelin_min_rating: float = 3.5
    michelin_min_reviews: int = 5
    # Google fiyatı bir öğündür. Günlük bütçe öğle + akşam + daha ucuz kahvaltı.
    meals_per_day: str = "2.5"


class TargetSettings(BaseModel):
    # scrape_targets.source_url yalnız bu alanlara https ile gidebilir. Boş liste hepsini reddeder.
    allowed_hosts: list[str] = Field(default_factory=list)


class ProvidersSettings(BaseModel):
    obilet: ObiletSettings = Field(default_factory=ObiletSettings)
    obilet_hotel: ObiletHotelSettings = Field(default_factory=ObiletHotelSettings)
    airbnb: AirbnbSettings = Field(default_factory=AirbnbSettings)
    fuel: FuelSettings = Field(default_factory=FuelSettings)
    osm: OsmSettings = Field(default_factory=OsmSettings)
    google_places: GooglePlacesSettings = Field(default_factory=GooglePlacesSettings)
    targets: TargetSettings = Field(default_factory=TargetSettings)


class Settings(BaseModel):
    vacation_api: VacationApiSettings = Field(default_factory=VacationApiSettings)
    server: ServerSettings = Field(default_factory=ServerSettings)
    limits: LimitSettings = Field(default_factory=LimitSettings)
    scheduler: SchedulerSettings = Field(default_factory=SchedulerSettings)
    route_targets: list[RouteTarget] = Field(default_factory=list)
    providers: ProvidersSettings = Field(default_factory=ProvidersSettings)


def read_env_files(paths: tuple[Path, ...] = ENV_FILES) -> dict[str, str]:
    values: dict[str, str] = {}
    for path in paths:
        if not path.is_file():
            continue
        for raw in path.read_text(encoding="utf-8-sig").splitlines():
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            if line.startswith("export "):
                line = line[len("export "):].strip()
            key, sep, value = line.partition("=")
            key, value = key.strip(), value.strip()
            if not sep or not key:
                continue
            if len(value) >= 2 and value[0] == value[-1] and value[0] in "'\"":
                value = value[1:-1]
            values.setdefault(key, value)
    return values


def load_settings(path: Path | None = None, env_files: tuple[Path, ...] = ENV_FILES) -> Settings:
    config_path = path or Path(os.environ.get("SCRAPER_CONFIG", DEFAULT_CONFIG_PATH))
    raw = yaml.safe_load(config_path.read_text(encoding="utf-8")) or {}
    settings = Settings.model_validate(raw)

    env = {**read_env_files(env_files), **{k: v for k, v in os.environ.items() if v}}
    if url := env.get("VACATION_API_URL"):
        settings.vacation_api.base_url = url
    if key := env.get("SCRAPER_API_KEY"):
        settings.vacation_api.scraper_key = key
    if key := env.get("SCRAPER_TRIGGER_KEY"):
        settings.server.trigger_key = key
    if key := env.get("GOOGLE_PLACES_API_KEY"):
        settings.providers.google_places.api_key = key
    return settings


@lru_cache
def get_settings() -> Settings:
    return load_settings()
