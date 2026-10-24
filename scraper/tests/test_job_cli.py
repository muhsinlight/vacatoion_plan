import json

import pytest

from app.jobs.run import main
from app.jobs import job_bus
from app.settings import Settings


def test_food_dry_run_exits_zero(monkeypatch, capsys):
    monkeypatch.setattr("app.jobs.run.get_settings", lambda: Settings())
    assert main(["--job", "food", "--dry-run"]) == 0
    payload = json.loads(capsys.readouterr().out)
    assert payload["job"] == "food"
    assert payload["dry_run"] is True
    assert payload["errors"] == []


def test_unknown_job_exits_with_argparse_error():
    with pytest.raises(SystemExit) as caught:
        main(["--job", "missing"])
    assert caught.value.code == 2


def test_bus_without_targets_records_error():
    result = job_bus(Settings(), dry_run=True)
    assert result.collected == 0
    assert result.saved == 0
    assert result.errors == ["route_targets boş"]
