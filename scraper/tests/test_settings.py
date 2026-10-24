from app.settings import OsmSettings, load_settings


def test_overpass_endpoints_keep_primary_and_drop_duplicates():
    osm = OsmSettings(
        overpass_url="https://primary.example/interpreter",
        overpass_fallbacks=[
            "https://primary.example/interpreter",
            "https://backup.example/interpreter",
        ],
    )
    assert osm.overpass_endpoints() == [
        "https://primary.example/interpreter",
        "https://backup.example/interpreter",
    ]


def test_env_file_fills_keys_and_process_env_wins(tmp_path, monkeypatch):
    config = tmp_path / "settings.yaml"
    config.write_text("vacation_api:\n  base_url: http://yaml\n", encoding="utf-8")
    env_file = tmp_path / ".env"
    env_file.write_text(
        "VACATION_API_URL=http://from-file\nSCRAPER_TRIGGER_KEY=file-key\n",
        encoding="utf-8",
    )
    for name in ("VACATION_API_URL", "SCRAPER_API_KEY", "SCRAPER_TRIGGER_KEY", "GOOGLE_PLACES_API_KEY"):
        monkeypatch.delenv(name, raising=False)

    from_file = load_settings(config, env_files=(env_file,))
    assert from_file.vacation_api.base_url == "http://from-file"
    assert from_file.server.trigger_key == "file-key"

    monkeypatch.setenv("VACATION_API_URL", "http://from-os")
    from_os = load_settings(config, env_files=(env_file,))
    assert from_os.vacation_api.base_url == "http://from-os"
