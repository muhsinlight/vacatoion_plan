from datetime import date, datetime, timezone
from decimal import Decimal
from pathlib import Path

from scrapling.parser import Selector

from app.providers.obilet_bus import build_observation, is_journey_list_url, parse_journeys, scrape_date, search_url
from app.settings import ObiletSettings, RouteTarget

FIXTURE = Path(__file__).parent / "fixtures" / "obilet_search.html"
ROUTE = RouteTarget(origin="aksaray", dest="isparta", obilet_origin_id=414, obilet_dest_id=381)


def load_page() -> Selector:
    return Selector(FIXTURE.read_text(encoding="utf-8"))


def test_landing_page_is_not_a_journey_list():
    assert is_journey_list_url('https://www.obilet.com/seferler/371-425/2026-10-21')
    assert not is_journey_list_url('https://www.obilet.com/otobus-bileti/diyarbakir-kilis')


def test_scrape_date_shifts_past_days_to_today():
    today = date(2026, 10, 23)
    assert scrape_date(date(2026, 10, 21), today) == today
    assert scrape_date(date(2026, 10, 28), today) == date(2026, 10, 28)


def test_parse_journeys_skips_template_and_reads_prices():
    page = load_page()
    # Canlı kartta ilk metin düğümü boşluk; saat sonraki düğümde. .get() onu kaçırır.
    assert not (page.css(".departure::text").get() or "").strip()
    journeys = parse_journeys(page)

    assert [j.price for j in journeys] == [Decimal("800.00"), Decimal("700.50"), Decimal("640.00")]
    assert journeys[0].partner == "Metro Turizm"
    assert not any(j.full for j in journeys)


def test_build_observation_excludes_full_journeys():
    checked_at = datetime(2026, 10, 14, 13, 0, tzinfo=timezone.utc)
    obs = build_observation(ROUTE, parse_journeys(load_page()), date(2026, 10, 28), "https://x", checked_at)

    assert (obs.min, obs.typical, obs.max) == (Decimal("640.00"), Decimal("700.50"), Decimal("800.00"))
    assert obs.sample_size == 3
    assert obs.week_bucket == date(2026, 10, 26)

    body = obs.model_dump(mode="json", by_alias=True, exclude_none=True)
    assert body["scopeType"] == "ROUTE"
    assert body["scopeKey"] == "aksaray|isparta|bus"
    assert body["unit"] == "PER_PERSON"
    assert body["transportMode"] == "BUS"
    assert body["originKey"] == "aksaray"
    assert body["weekBucket"] == "2026-10-26"
    assert body["min"] == 640.0
    assert body["currency"] == "TRY"
    assert obs.provider == "Su Turizm"
    assert obs.source_url == "https://x"
    assert body["provider"] == "Su Turizm"
    assert body["sourceUrl"] == "https://x"


def test_search_url():
    assert search_url(ObiletSettings(), ROUTE, date(2026, 10, 28)) == \
        "https://www.obilet.com/seferler/414-381/2026-10-28"
