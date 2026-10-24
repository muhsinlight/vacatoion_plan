import time

import httpx

from app.models import PriceObservation, PriceObservationBatchResponse, ScrapeIngestRequest, ScrapeTarget
from app.security import HEADER, is_usable
from app.settings import VacationApiSettings

_RETRYABLE_STATUS = {408, 429, 500, 502, 503, 504}
_MAX_ATTEMPTS = 3
_BACKOFF_SECONDS = 0.5


class VacationApiClient:
    def __init__(self, settings: VacationApiSettings, transport: httpx.BaseTransport | None = None):
        if not is_usable(settings.scraper_key):
            raise RuntimeError("SCRAPER_API_KEY yapılandırılmamış; API'ye yazılamaz")
        self._client = httpx.Client(
            base_url=settings.base_url.rstrip("/") + "/api/v1/scraper",
            headers={HEADER: settings.scraper_key},
            timeout=settings.timeout_seconds,
            transport=transport,
            follow_redirects=False,
        )

    def __enter__(self) -> "VacationApiClient":
        return self

    def __exit__(self, *exc) -> None:
        self._client.close()

    def _request(self, method: str, path: str, **kwargs) -> httpx.Response:
        last_error: Exception | None = None
        for attempt in range(_MAX_ATTEMPTS):
            try:
                response = self._client.request(method, path, **kwargs)
            except httpx.TransportError as ex:
                last_error = ex
                if attempt < _MAX_ATTEMPTS - 1:
                    time.sleep(_BACKOFF_SECONDS * (2**attempt))
                    continue
                raise
            if response.status_code in _RETRYABLE_STATUS and attempt < _MAX_ATTEMPTS - 1:
                time.sleep(_BACKOFF_SECONDS * (2**attempt))
                continue
            response.raise_for_status()
            return response
        if last_error is not None:
            raise last_error
        raise RuntimeError("API isteği tamamlanamadı")

    def post_observations(self, observations: list[PriceObservation]) -> PriceObservationBatchResponse:
        body = {"observations": [o.model_dump(mode="json", by_alias=True, exclude_none=True) for o in observations]}
        response = self._request("POST", "/observations", json=body)
        return PriceObservationBatchResponse.model_validate(response.json())

    def list_targets(self) -> list[ScrapeTarget]:
        response = self._request("GET", "/targets")
        return [ScrapeTarget.model_validate(item) for item in response.json()]

    def ingest_target(self, request: ScrapeIngestRequest) -> dict:
        response = self._request(
            "POST",
            "/ingest",
            json=request.model_dump(mode="json", by_alias=True, exclude_none=True),
        )
        return response.json()
