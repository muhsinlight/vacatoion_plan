import time

import httpx

RETRYABLE_STATUS = {408, 429, 500, 502, 503, 504}
MAX_ATTEMPTS = 3
BACKOFF_SECONDS = 0.5


def request_with_retry(
        client: httpx.Client,
        method: str,
        url: str,
        *,
        attempts: int = MAX_ATTEMPTS,
        **kwargs,
) -> httpx.Response:
    last_error: Exception | None = None
    response: httpx.Response | None = None
    for attempt in range(attempts):
        try:
            response = client.request(method, url, **kwargs)
        except httpx.TransportError as ex:
            last_error = ex
            if attempt >= attempts - 1:
                raise
            time.sleep(BACKOFF_SECONDS * (2**attempt))
            continue
        if response.status_code in RETRYABLE_STATUS and attempt < attempts - 1:
            time.sleep(BACKOFF_SECONDS * (2**attempt))
            continue
        return response
    if last_error is not None:
        raise last_error
    if response is None:
        raise RuntimeError("İstek tamamlanamadı")
    return response
