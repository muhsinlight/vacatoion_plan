import httpx

from app.providers.osm_food import _geocode, _named_places, _place_point, fetch_places
from app.settings import OsmSettings


def test_geocode_asks_for_the_city():
    seen: dict[str, str] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen["city"] = request.url.params["city"]
        seen["country"] = request.url.params["country"]
        return httpx.Response(200, json=[{"lat": "37.9698490", "lon": "34.6764495", "addresstype": "city"}])

    with httpx.Client(transport=httpx.MockTransport(handler)) as client:
        point = _geocode(OsmSettings(), "Niğde", client)

    assert seen == {"city": "Niğde", "country": "Türkiye"}
    assert point == (37.9698490, 34.6764495)


def test_place_point_skips_province_centroid():
    point = _place_point([
        {"lat": "38.0664691", "lon": "34.7051438", "addresstype": "province"},
        {"lat": "37.9698490", "lon": "34.6764495", "addresstype": "city"},
    ])

    assert point == (37.9698490, 34.6764495)


def test_named_places_skips_blank_and_duplicate_names():
    elements = [
        {"type": "node", "id": 1, "tags": {"name": "Kebapçı"}},
        {"type": "node", "id": 2, "tags": {"amenity": "restaurant"}},
        {"type": "way", "id": 3, "tags": {"name": "kebapçı"}},
        {"type": "way", "id": 9, "tags": {"name:tr": "Lokanta"}},
    ]

    places = _named_places(elements, limit=8)

    assert [(place.name, place.url) for place in places] == [
        ("Kebapçı", "https://www.openstreetmap.org/node/1"),
        ("Lokanta", "https://www.openstreetmap.org/way/9"),
    ]


def test_fetch_places_skips_a_down_overpass_server():
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.host == "nominatim.openstreetmap.org":
            return httpx.Response(200, json=[{"lat": "39.9", "lon": "32.8"}])
        if request.url.host == "overpass-api.de":
            return httpx.Response(502, json={"remark": "down"})
        return httpx.Response(200, json={"elements": [
            {"type": "node", "id": 7, "tags": {"name": "Yedek"}},
        ]})

    settings = OsmSettings(
        overpass_url="https://overpass-api.de/api/interpreter",
        overpass_fallbacks=["https://overpass.kumi.systems/api/interpreter"],
    )
    with httpx.Client(transport=httpx.MockTransport(handler)) as client:
        result = fetch_places(settings, "Ankara", client)

    assert result.errors == []
    assert result.places[0].name == "Yedek"


def test_fetch_places_skips_a_timed_out_overpass_server():
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.host == "nominatim.openstreetmap.org":
            return httpx.Response(200, json=[{"lat": "39.9", "lon": "32.8"}])
        if request.url.host == "overpass-api.de":
            raise httpx.ReadTimeout("slow", request=request)
        return httpx.Response(200, json={"elements": [
            {"type": "node", "id": 8, "tags": {"name": "Hızlı"}},
        ]})

    settings = OsmSettings(
        overpass_url="https://overpass-api.de/api/interpreter",
        overpass_fallbacks=["https://overpass.kumi.systems/api/interpreter"],
    )
    with httpx.Client(transport=httpx.MockTransport(handler)) as client:
        result = fetch_places(settings, "Ankara", client)

    assert result.errors == []
    assert result.places[0].name == "Hızlı"


def test_fetch_places_widens_the_radius_when_the_center_is_sparse():
    queries: list[bytes] = []

    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.host == "nominatim.openstreetmap.org":
            return httpx.Response(200, json=[{"lat": "40.46", "lon": "39.47", "addresstype": "city"}])
        queries.append(request.content)
        if b"around:6000" in request.content:
            return httpx.Response(200, json={"elements": [
                {"type": "node", "id": 1, "tags": {"name": "Simit Sarayı", "amenity": "cafe"}},
            ]})
        return httpx.Response(200, json={"elements": [
            {"type": "node", "id": 1, "tags": {"name": "Simit Sarayı", "amenity": "cafe"}},
            {"type": "node", "id": 2, "tags": {"name": "Çiçek Park", "amenity": "restaurant"}},
            {"type": "way", "id": 3, "tags": {"name": "Ernail Yeri", "amenity": "restaurant"}},
        ]})

    settings = OsmSettings(
        overpass_url="https://overpass-api.de/api/interpreter",
        overpass_fallbacks=[],
    )
    with httpx.Client(transport=httpx.MockTransport(handler)) as client:
        result = fetch_places(settings, "Gümüşhane", client)

    assert any(b"around:15000" in query for query in queries)
    assert [place.name for place in result.places] == ["Simit Sarayı", "Çiçek Park", "Ernail Yeri"]
    assert result.errors == []


def test_fetch_places_reads_nominatim_then_overpass():
    def handler(request: httpx.Request) -> httpx.Response:
        if request.url.host == "nominatim.openstreetmap.org":
            return httpx.Response(200, json=[{"lat": "39.9", "lon": "32.8"}])
        assert b"amenity" in request.content
        return httpx.Response(200, json={"elements": [
            {"type": "node", "id": 42, "tags": {"name": "Sofrası"}},
        ]})

    settings = OsmSettings(
        nominatim_url="https://nominatim.openstreetmap.org/search",
        overpass_url="https://overpass-api.de/api/interpreter",
    )
    with httpx.Client(transport=httpx.MockTransport(handler)) as client:
        result = fetch_places(settings, "Ankara", client)

    assert result.city_key == "ankara"
    assert result.errors == []
    assert result.places[0].name == "Sofrası"
    assert result.places[0].url == "https://www.openstreetmap.org/node/42"
