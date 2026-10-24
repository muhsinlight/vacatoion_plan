---
name: start-stack
description: >-
  Vacation Plan yığınını elle başlatır (kök start.ps1 yok). Tarayıcı
  http://localhost:5173. Projeyi çalıştır veya ayağa kaldır dendiğinde README
  Çalıştırma bölümündeki sırayı uygula.
---

# Yığını başlat

Kökte `start.ps1` **yok**. Kullanıcıya README’deki sırayı özetle ver; gerekirse komutları çalıştır.

1. **PostgreSQL** — `api/` içinde: `docker compose -f compose.yaml up -d --wait postgres` (5433).
2. **Ortam** — kök `.env` veya `api/.env`: `JWT_SECRET`, `SCRAPER_API_KEY`, `SCRAPER_TRIGGER_KEY` (≥32 karakter).
3. **API** — `api/`: `.\mvnw.cmd spring-boot:run` (8080).
4. **Scraper** — `scraper/`: `.venv\Scripts\uvicorn app.main:app --host 127.0.0.1 --port 8081` (8081).
5. **Arayüz** — `frontend/`: `npm run dev` (5173).

Port zaten dinleniyorsa o süreci yeniden başlatma. Verilecek adres: `http://localhost:5173`.

Üretim notları: `docs/deploy.md`.
