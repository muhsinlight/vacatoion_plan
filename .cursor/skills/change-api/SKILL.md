---
name: change-api
description: >-
  Vacation Plan API katmanına uç, bütçe, oturum veya şema ekler. Java,
  controller, service, Liquibase, JWT veya önizleme hesabı değişince uygula.
  Thymeleaf veya uydurma fiyat bandı ekleme.
---

# API değişikliği

Önce `source-map`. Akış `docs/sistem.md` ve `docs/onizleme.md`. Ayar kök `README.md` içindeki `vacation.*` tablosu.

Katman: `controller` alır, `service` sözleşmeyi söyler, `service/impl` yapar, `repository` satırı okur. JSON `dto` olur. JPA `entity` HTTP gövdesi olmaz.

- Şema yalnız Liquibase (`api/src/main/resources/db/changelog/`). `ddl-auto` validate kalır.
- `destinations` satırı yoksa 404. Uydurma fiyat bandı yok.
- Oturum JWT çerezidir. Yeni `/api/v1/**` ucu oturum ister; anonim uçlar `docs/sistem.md` listesindedir. `/api/v1/scraper/**` ayrıca `X-Scraper-Key` ister.
- Anahtarlar `.env` içindedir. `JWT_SECRET`, `SCRAPER_API_KEY`, `SCRAPER_TRIGGER_KEY` en az 32 karakter.
- API yalnız JSON sunar. Thymeleaf, htmx ve `start.ps1` yok.

Test: `api/` içinde `.\mvnw.cmd -B test`.
