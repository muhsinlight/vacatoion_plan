# Scraper

Harita [sistem.md](sistem.md) içindedir. İki süreç birbirini çağırır. Başlık iki yönde de `X-Scraper-Key` olur. Değer farklıdır.

| Yön | Değer | Kim gönderir |
|-----|--------|----------------|
| Scraper → API | `SCRAPER_API_KEY` | `scraper/app/vacation_api.py` içindeki `VacationApiClient` |
| API → scraper | `SCRAPER_TRIGGER_KEY` | `BusRouteFareServiceImpl`, `LodgingOfferServiceImpl` |

Anahtarlar kök `.env` içindedir. `scraper/config/settings.yaml` anahtar taşımaz. Boş veya 32 karakterden kısa anahtar ilgili ucu 503 yapar. API adresi scraper ayarında `vacation_api.base_url` (varsayılan `http://127.0.0.1:8080`). Scraper adresi API’de `vacation.scraper-base-url` (varsayılan `http://127.0.0.1:8081`). API’nin bekleme süresi `vacation.scraper-read-timeout` (varsayılan 210 saniye).

İl slug’ı ortaktır. API `tr_provinces.json` ile 81 il anahtarına iner. Scraper gelen anahtarı yeniden üretmez.

## Scraper’ın açtığı uçlar

`scraper/app/main.py`. Tetik anahtarı ister. Zamanlayıcı bu uçları çağırmaz; önizleme çağırır.

| Method | Path | Ne olur |
|--------|------|---------|
| GET | `/health` | Süreç ve zamanlayıcı. Anahtar istemez. |
| POST | `/routes/bus` | İstenen gidiş ve dönüş tarihlerinde Obilet bandı, sonra gözlem olarak API’ye yazılır |
| POST | `/lodging/search` | Otel, pansiyon veya kiralık bant ve üç kademe |
| POST | `/food/search` | OpenStreetMap: şehir merkezindeki restoran adları. Günlük tutar katalog bandında kalır |

Önbellek `route-quote-max-age` veya `lodging-quote-max-age` içindeyse API bu uçları çağırmaz. İkisi de varsayılan 12 saattir. Çağrı `SCRAPER` kota kovasına girer (saatte 20).

## API’nin scraper’a açtığı uçlar

Taban yol `/api/v1/scraper`. Filtre `ScraperKeyFilter`.

| Method | Path | Ne olur |
|--------|------|---------|
| GET | `/targets` | Etkin ve scrape kabul eden `scrape_targets` satırları |
| POST | `/observations` | Satır `price_observations` olur. `ROUTE` kapsamı `RouteQuoteMaterializer` ile `route_quotes` olur. Ulusal yakıt `fuel_prices` olur. |
| POST | `/ingest` | Tek hedefin sayfa sonucu kataloğa işlenir |

## Zamanlanmış işler

`scraper/app/jobs`. Saat dilimi `Europe/Istanbul`. Cron `settings.yaml` içindedir. `SCRAPER_SCHEDULER_ENABLED=false` zamanlayıcıyı kapatır. Saatlik site çekimi `limits.site_fetches_per_hour` (varsayılan 60) ile sınırlıdır.

| İş | Kaynak | Varsayılan | API’ye giden |
|----|--------|------------|----------------|
| `bus` | `route_targets` ve Obilet | Pazartesi 03:00 | `POST /observations` |
| `fuel` | Yakıt tablosu | Ayın 1’i 04:00 | `POST /observations` |
| `food` | OpenStreetMap, önizlemede | Zamanlanmış tur yok | Restoran listesi `destinations` kaydına yazılır. Tutar katalog bandıdır |
| `targets` | `GET /targets`, sonra sayfa | Her gün 05:00 | `POST /ingest` |

`targets` işi `providers.targets.allowed_hosts` boşsa hiçbir adrese gitmez.

| Yer | İş |
|-----|-----|
| `scraper/app/providers` | Obilet otobüs ve otel, Airbnb, yakıt, OpenStreetMap yemek listesi, hedef sayfası |
| `scraper/config/settings.yaml` | Cron, rotalar, sağlayıcı adresleri |
