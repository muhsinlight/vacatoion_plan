---
name: run-tests
description: >-
  Vacation Plan testlerini ilgili modülde çalıştırır. Test, pytest, mvnw test,
  npm test veya CI dendiğinde yalnız o modülün komutunu kullan. Yığını
  başlatmak test sayılmaz.
---

# Test

Kökte tek test komutu yok. CI `.github/workflows/ci.yml` üç işi ayırır. Değişen modülü çalıştır.

| Modül | Dizin | Komut |
|-------|--------|--------|
| API | `api/` | `.\mvnw.cmd -B test` |
| Scraper | `scraper/` | `.venv\Scripts\python -m pytest` |
| Arayüz | `frontend/` | `npm test` |

Arayüzün üretim derlemesi ayrıca `frontend/` içinde `npm run build`. Scraper sanal ortamı yoksa önce `py -3.12 -m venv .venv` ve `.venv\Scripts\pip install -e ".[dev]"`.

Postgres şema testi `PostgresSchemaIT` Docker ister. Konteyner yoksa testi atlama; çıkan hatayı yaz. Yığını açmak için `start-stack`.
