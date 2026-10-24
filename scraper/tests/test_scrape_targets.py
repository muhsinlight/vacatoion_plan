import httpx
import pytest

from app.models import ScrapeTarget
from app.providers.scrape_targets import check_source_url, fetch_target

ALLOWED = ["prices.example.com"]


@pytest.mark.parametrize("url", [
    "http://prices.example.com/a.json",
    "https://127.0.0.1/a.json",
    "https://localhost/a.json",
    "https://169.254.169.254/latest/meta-data",
    "https://prices.example.com.evil.test/a.json",
    "https://user:pw@prices.example.com/a.json",
    "https://prices.example.com:8443/a.json",
    "file:///etc/passwd",
])
def test_rejects_urls_outside_allowlist(url):
    with pytest.raises(ValueError):
        check_source_url(url, ALLOWED)


def test_accepts_allowed_https_host():
    check_source_url("https://Prices.Example.com/a.json", ALLOWED)


def test_empty_allowlist_rejects_everything():
    with pytest.raises(ValueError):
        check_source_url("https://prices.example.com/a.json", [])


def test_does_not_follow_redirects():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(302, headers={"Location": "http://127.0.0.1:5433/"})

    target = ScrapeTarget(id=1, city_key="isparta", catalog_field="ACTIVITY_ENTRY",
                          source_url="https://prices.example.com/a.json", parser_type="JSON_MIN_MAX", label="x")
    with httpx.Client(transport=httpx.MockTransport(handler)) as client, pytest.raises(httpx.HTTPStatusError):
        fetch_target(target, client, ALLOWED)
