import logging
import os
import re
import time
from pathlib import Path
from collections.abc import Callable
from dataclasses import dataclass
from datetime import date, datetime, time as civil_time, timedelta, timezone
from decimal import Decimal

from scrapling.fetchers import StealthySession
from scrapling.parser import Selector

from app.bands import band_from_prices, parse_tr_amount, week_bucket
from app.fetch_budget import LIMIT_ERROR
from app.models import BudgetCategory, PriceObservation, PriceScopeType, PriceSourceType, PriceUnit, TransportMode
from app.settings import ObiletSettings, RouteTarget

log = logging.getLogger(__name__)

# Sefer kartları /json/journeys XHR'ı ile istemci tarafında çiziliyor; düz HTTP isteği bot korumasına takılıyor.
JOURNEY_SELECTOR = "ul#list li.journey[id^='journey-']"
# Sefer kartı ya da "sefer bulunamadı" satırı.
LIST_READY_SELECTOR = f"{JOURNEY_SELECTOR}, ul#list li.item.single"
# Sefer yokken Obilet /otobus-bileti/ iniş sayfasına düşer; 60 sn beklenmez.
LIST_WAIT_MS = 20_000
JOURNEY_ID = re.compile(r"^journey-\d+$")
DEPARTURE_CLOCK = re.compile(r"(\d{1,2}):(\d{2})")
# Liste kalkış saatine göre artar. 08:00 ve 22:00 banda girer; ilk 22:00 sonrası kartta okuma biter.
DEPARTURE_START = civil_time(8, 0)
DEPARTURE_END = civil_time(22, 0)
# Çerez bandı ve benzeri katmanlar. Sefer kartlarının içinde değiller; kapatılmazsa da fiyat okunur.
OVERLAY_DISMISS = (
    ".cookies-policy button.close",
    ".cookies-policy a.cancel",
    "[class*='popup'] button.close",
    "[class*='modal'] button.close",
)


@dataclass(frozen=True)
class Journey:
    journey_id: str
    partner: str | None
    price: Decimal
    full: bool


def departure_clock(item: Selector) -> civil_time | None:
    # İlk metin düğümü boşluk; saat ".departure" içindeki sonraki düğümde.
    raw = " ".join(item.css(".departure::text").getall())
    if not raw.strip():
        return None
    match = DEPARTURE_CLOCK.search(raw)
    if not match:
        return None
    hour, minute = int(match.group(1)), int(match.group(2))
    if hour > 23 or minute > 59:
        return None
    return civil_time(hour, minute)


def parse_journeys(page: Selector) -> list[Journey]:
    """08:00–22:00 kalkışlar. Daha erken kartta yalnız saat okunur; 22:00 sonrası liste kesilir."""
    journeys = []
    for item in page.css(JOURNEY_SELECTOR):
        departure = departure_clock(item)
        if departure is None or departure < DEPARTURE_START:
            continue
        if departure > DEPARTURE_END:
            break
        journey_id = item.attrib.get("id", "")
        if not JOURNEY_ID.match(journey_id):
            continue
        integer = item.css(".price .amount-integer::text").get()
        if not integer:
            continue
        decimal = item.css(".price .amount-decimal::text").get() or ""
        journeys.append(Journey(
            journey_id=journey_id,
            partner=item.css(".partner-logo::attr(data-name)").get(),
            price=parse_tr_amount(integer.strip() + decimal.strip()),
            full=item.has_class("full"),
        ))
    return journeys


def cheapest_journey(journeys: list[Journey]) -> Journey:
    """Dolu seferler elenir. Hepsi doluysa en düşük fiyatlı kart kalır."""
    available = [journey for journey in journeys if not journey.full]
    return min(available or journeys, key=lambda journey: journey.price)


def build_observation(
        route: RouteTarget,
        journeys: list[Journey],
        travel_date: date,
        source_url: str,
        checked_at: datetime,
) -> PriceObservation:
    available = [j.price for j in journeys if not j.full]
    band = band_from_prices(available or [j.price for j in journeys])
    cheapest = cheapest_journey(journeys)
    return PriceObservation(
        category=BudgetCategory.TRANSPORT,
        scope_type=PriceScopeType.ROUTE,
        scope_key=f"{route.origin}|{route.dest}|bus",
        unit=PriceUnit.PER_PERSON,
        min=band.min,
        typical=band.typical,
        max=band.max,
        source_type=PriceSourceType.SCRAPE,
        provider=cheapest.partner,
        source_url=source_url,
        sample_size=band.sample_size,
        confidence=band.confidence,
        checked_at=checked_at,
        origin_key=route.origin,
        dest_key=route.dest,
        transport_mode=TransportMode.BUS,
        week_bucket=week_bucket(travel_date),
    )


def ensure_chromium() -> None:
    """Sandbox önbelleğinde Chromium yoksa kurulu Playwright dizinine döner."""
    def has_chrome(root: Path) -> bool:
        if not root.is_dir():
            return False
        return any(root.glob("chromium-*/chrome-win64/chrome.exe")) or any(
            root.glob("chromium-*/chrome-linux/chrome")
        )

    current = os.environ.get("PLAYWRIGHT_BROWSERS_PATH", "")
    if current and has_chrome(Path(current)):
        return
    local = os.environ.get("LOCALAPPDATA")
    if not local:
        return
    fallback = Path(local) / "ms-playwright"
    if has_chrome(fallback):
        os.environ["PLAYWRIGHT_BROWSERS_PATH"] = str(fallback)


def dismiss_overlays(page) -> None:
    """Görünen çerez / popup katmanını kapatır. Yoksa veya tıklanmazsa sefer okuması sürer."""
    for selector in OVERLAY_DISMISS:
        try:
            button = page.locator(selector).first
            if button.is_visible(timeout=500):
                button.click(timeout=2000)
                log.info("Katman kapatıldı: %s", selector)
                return
        except Exception:
            continue


def search_url(settings: ObiletSettings, route: RouteTarget, travel_date: date) -> str:
    return settings.search_url.format(
        origin_id=route.obilet_origin_id,
        dest_id=route.obilet_dest_id,
        date=travel_date.isoformat(),
    )


def is_journey_list_url(url: str) -> bool:
    return "/seferler/" in (url or "")


def scrape_date(travel_date: date, today: date | None = None) -> date:
    """Obilet geçmiş güne sefer basmaz; fiyat bugünden okunur."""
    day = today or date.today()
    return travel_date if travel_date >= day else day


def scrape_routes(
        settings: ObiletSettings,
        routes: list[RouteTarget],
        today: date | None = None,
        acquire: Callable[[], bool] | None = None,
) -> tuple[list[PriceObservation], list[str]]:
    """Tek yön, kişi başı fiyat bandı. Rota hataları toplanır, batch durdurulmaz."""
    travel_date = (today or date.today()) + timedelta(days=settings.sample_days_ahead)
    return scrape_legs(settings, [(route, travel_date) for route in routes], acquire=acquire)


def scrape_legs(
        settings: ObiletSettings,
        legs: list[tuple[RouteTarget, date]],
        acquire: Callable[[], bool] | None = None,
) -> tuple[list[PriceObservation], list[str]]:
    """Her bacak kendi seyahat tarihinde. Bir rotanın hatası diğerlerini durdurmaz.

    ``acquire`` False dönerse kalan bacaklar sayfa açılmadan hata olarak yazılır.
    """
    observations: list[PriceObservation] = []
    errors: list[str] = []
    if acquire is not None:
        allowed = []
        for route, travel_date in legs:
            if acquire():
                allowed.append((route, travel_date))
            else:
                errors.append(f"{route.origin}->{route.dest} {travel_date.isoformat()}: {LIMIT_ERROR}")
        legs = allowed
    if not legs:
        return observations, errors

    ensure_chromium()
    # Sefer listesi sayfa açıldıktan sonra /json/journeys ile çiziliyor.
    # Kaynakları kesmek bu isteği de keser ve liste boş kalır.
    with StealthySession(
            headless=settings.headless,
            disable_resources=False,
            block_ads=True,
            timeout=min(settings.timeout_ms, LIST_WAIT_MS),
    ) as session:
        for index, (route, travel_date) in enumerate(legs):
            if index > 0:
                time.sleep(settings.delay_between_routes_seconds)
            lookup_date = scrape_date(travel_date)
            url = search_url(settings, route, lookup_date)
            label = f"{route.origin}->{route.dest} {lookup_date.isoformat()}"
            try:
                page = None
                last_error: Exception | None = None
                for attempt in range(3):
                    try:
                        page = session.fetch(
                            url,
                            page_action=dismiss_overlays,
                            wait_selector=LIST_READY_SELECTOR,
                        )
                        last_error = None
                        break
                    except Exception as ex:
                        last_error = ex
                        if attempt < 2:
                            time.sleep(0.5 * (2**attempt))
                if page is None:
                    raise last_error or RuntimeError("sayfa alınamadı")
                journeys = parse_journeys(page)
                if not journeys:
                    errors.append(f"{label}: Obilet bu güzergahta sefer listelemiyor")
                    continue
                observations.append(build_observation(
                    route, journeys, travel_date, url, datetime.now(timezone.utc)))
                log.info("%s: %d sefer", label, len(journeys))
            except Exception as ex:
                log.exception("%s başarısız", label)
                errors.append(f"{label}: {ex}")

    return observations, errors
