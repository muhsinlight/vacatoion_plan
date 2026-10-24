from unittest.mock import patch

from app.food_search import search_food
from app.models import FoodSearchRequest
from app.providers.osm_food import OsmFoodResult, OsmPlace
from app.settings import Settings


def test_search_food_maps_osm_places():
    result = OsmFoodResult(
        city_key="bayburt",
        places=[OsmPlace("Lokanta", "https://www.openstreetmap.org/node/1")],
        errors=[],
    )
    with patch("app.food_search.osm_food.fetch_places", return_value=result) as fetch:
        response = search_food(Settings(), FoodSearchRequest(city="Bayburt"))

    fetch.assert_called_once()
    assert response.city_key == "bayburt"
    assert response.provider == "openstreetmap"
    assert response.band is None
    assert response.places[0].name == "Lokanta"
    assert response.places[0].url == "https://www.openstreetmap.org/node/1"
    assert response.errors == []


def test_search_food_returns_osm_errors():
    result = OsmFoodResult(city_key="bayburt", places=[], errors=["bayburt: restoran bulunamadı"])
    with patch("app.food_search.osm_food.fetch_places", return_value=result) as fetch:
        response = search_food(Settings(), FoodSearchRequest(city="Bayburt"))

    fetch.assert_called_once()
    assert response.provider is None
    assert response.band is None
    assert response.places == []
    assert response.errors == ["bayburt: restoran bulunamadı"]
