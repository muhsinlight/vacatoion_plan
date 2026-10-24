from pathlib import Path

from app.providers.obilet_locations import city_slug, parse_route_ids

FIXTURE = Path(__file__).parent / "fixtures" / "obilet_route_ids.html"


def test_parse_route_ids_uses_page_json_not_related_links():
    origin, dest = parse_route_ids(FIXTURE.read_text(encoding="utf-8"))
    assert (origin, dest) == (382, 356)


def test_city_slug_folds_turkish_characters():
    assert city_slug("  Isparta / Eğirdir ") == "isparta-egirdir"
    assert city_slug("Mersin") == "mersin"
    assert city_slug("İstanbul") == "istanbul"
    assert city_slug("Istanbul") == "istanbul"
