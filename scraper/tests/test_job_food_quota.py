from app.jobs import job_food
from app.settings import Settings


def test_job_food_does_not_fetch_provinces():
    result = job_food(Settings(), dry_run=True)

    assert result.job == "food"
    assert result.collected == 0
    assert result.errors == []
