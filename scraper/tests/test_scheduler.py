import logging

import pytest

from app.jobs import scheduler as scheduler_mod
from app.settings import SchedulerSettings, Settings


def test_unknown_job_is_rejected():
    settings = Settings(scheduler=SchedulerSettings(jobs={"nope": "0 0 * * *"}))
    with pytest.raises(ValueError, match="Bilinmeyen job"):
        scheduler_mod.build_scheduler(settings)


def test_configured_jobs_are_registered():
    settings = Settings(scheduler=SchedulerSettings(
        timezone="Europe/Istanbul",
        jobs={"food": "0 4 1 * *", "fuel": "15 5 * * *"},
    ))
    scheduler = scheduler_mod.build_scheduler(settings)
    assert {job.id for job in scheduler.get_jobs()} == {"food", "fuel"}


def test_job_failure_stays_inside_the_runner(monkeypatch, caplog):
    def boom(settings, dry_run):
        raise RuntimeError("patladı")

    monkeypatch.setitem(scheduler_mod.JOBS, "food", boom)
    settings = Settings(scheduler=SchedulerSettings(jobs={"food": "0 4 1 * *"}))
    scheduler = scheduler_mod.build_scheduler(settings)
    job = scheduler.get_job("food")
    with caplog.at_level(logging.ERROR):
        job.func(*job.args)
    assert "başarısız" in caplog.text
