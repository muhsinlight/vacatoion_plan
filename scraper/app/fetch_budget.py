"""Dış sitelere giden isteklerin saatlik sınırı. Zamanlanmış işler ve API tetiklemeleri aynı sayaçtan düşer."""

import threading
import time
from collections import deque
from collections.abc import Callable

from app.settings import Settings

WINDOW_SECONDS = 3600
LIMIT_ERROR = "saatlik dış site istek sınırı doldu"


class FetchBudget:
    def __init__(self, max_per_hour: int, clock: Callable[[], float] = time.monotonic):
        self.max_per_hour = max_per_hour
        self._clock = clock
        self._stamps: deque[float] = deque()
        self._lock = threading.Lock()

    def try_acquire(self) -> bool:
        if self.max_per_hour <= 0:
            return True
        with self._lock:
            now = self._clock()
            while self._stamps and now - self._stamps[0] >= WINDOW_SECONDS:
                self._stamps.popleft()
            if len(self._stamps) >= self.max_per_hour:
                return False
            self._stamps.append(now)
            return True


_shared: FetchBudget | None = None
_shared_lock = threading.Lock()


def shared_budget(settings: Settings) -> FetchBudget:
    global _shared
    limit = settings.limits.site_fetches_per_hour
    with _shared_lock:
        if _shared is None or _shared.max_per_hour != limit:
            _shared = FetchBudget(limit)
        return _shared
