from datetime import date

from app.fetch_budget import LIMIT_ERROR, FetchBudget
from app.providers import obilet_bus
from app.settings import ObiletSettings, RouteTarget


class FakeClock:
    def __init__(self):
        self.now = 0.0

    def __call__(self) -> float:
        return self.now


def test_budget_blocks_after_limit_and_frees_after_an_hour():
    clock = FakeClock()
    budget = FetchBudget(2, clock=clock)
    assert budget.try_acquire()
    assert budget.try_acquire()
    assert not budget.try_acquire()
    clock.now = 3600
    assert budget.try_acquire()


def test_zero_means_unlimited():
    budget = FetchBudget(0)
    assert all(budget.try_acquire() for _ in range(100))


def test_scrape_legs_skips_browser_when_budget_is_empty():
    route = RouteTarget(origin="aksaray", dest="isparta", obilet_origin_id=414, obilet_dest_id=381)
    observations, errors = obilet_bus.scrape_legs(
        ObiletSettings(), [(route, date(2026, 10, 29))], acquire=lambda: False)
    assert observations == []
    assert errors == [f"aksaray->isparta 2026-10-29: {LIMIT_ERROR}"]
