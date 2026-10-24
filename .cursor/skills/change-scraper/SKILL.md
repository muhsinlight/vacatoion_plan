---
name: change-scraper
description: >-
  Vacation Plan scraper işi, sağlayıcı veya HTTP ucu ekler. Obilet, yakıt,
  konaklama, yemek, APScheduler veya settings.yaml değişince uygula. Anahtarı
  yaml dosyasına yazma.
---

# Scraper değişikliği

Önce `source-map`. Akış `docs/scraper.md`. Kök `scraper/app`. Ayar `scraper/config/settings.yaml`.

| Yön | Ortam | Kim gönderir |
|-----|--------|----------------|
| Scraper → API | `SCRAPER_API_KEY` | `vacation_api.py` |
| API → scraper | `SCRAPER_TRIGGER_KEY` | API konaklama ve otobüs servisleri |

`settings.yaml` anahtar taşımaz. Boş veya 32 karakterden kısa anahtar ilgili ucu 503 yapar.

Yeni zamanlanmış iş `jobs/run.py`, `jobs/scheduler.py` ve `settings.yaml` cron satırına birlikte girer. Sağlayıcı kodu `app/providers` altındadır. `targets` işi `providers.targets.allowed_hosts` boşsa hiçbir adrese gitmez.

Tetik uçları (`/routes/bus`, `/lodging/search`, `/food/search`) önizleme çağırır; zamanlayıcı çağırmaz. Test: `scraper/` içinde `.venv\Scripts\python -m pytest`.
