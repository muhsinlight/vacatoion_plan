import logging

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger

from app.jobs import JOBS
from app.settings import Settings

log = logging.getLogger(__name__)


def _run(name: str, settings: Settings) -> None:
    try:
        result = JOBS[name](settings, False)
        log.info("Job %s bitti: %s", name, result.to_dict())
    except Exception:
        log.exception("Job %s başarısız", name)


def build_scheduler(settings: Settings) -> BackgroundScheduler:
    scheduler = BackgroundScheduler(timezone=settings.scheduler.timezone)
    for name, cron in settings.scheduler.jobs.items():
        if name not in JOBS:
            raise ValueError(f"Bilinmeyen job: {name}")
        if name == "targets" and not settings.providers.targets.allowed_hosts:
            log.warning("targets job atlandı: allowed_hosts boş")
            continue
        scheduler.add_job(
            _run,
            CronTrigger.from_crontab(cron, timezone=settings.scheduler.timezone),
            args=[name, settings],
            id=name,
            max_instances=1,
            coalesce=True,
        )
    return scheduler
