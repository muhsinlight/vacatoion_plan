# Vacation Plan

Akıllı tatil planlama: bütçe tahmini, günlük program taslağı, bütçe optimizasyon önerileri ve (isteğe bağlı) Groq ile etkinlik önerileri.

Harita: [docs/sistem.md](docs/sistem.md). Önizleme: [docs/onizleme.md](docs/onizleme.md). Scraper: [docs/scraper.md](docs/scraper.md).

## Gereksinimler

- Java 21
- Node.js 20+ (Vue arayüzü)
- Python 3.11+ (scraper)
- Docker Desktop (PostgreSQL)
- `GROQ_API_KEY` (etkinlik önerisi endpoint'i için, isteğe bağlı)

## Çalıştırma

Kökte `start.ps1` yok; süreçleri ayrı terminallerde açın. Tarayıcı: `http://localhost:5173`

**1. PostgreSQL** (`api/` içinden):

```powershell
cd api
docker compose -f compose.yaml up -d --wait postgres
```

**2. Ortam:** Kök `.env.example` dosyasını `.env` olarak kopyalayın (veya `api/.env`). En az `JWT_SECRET`, `SCRAPER_API_KEY`, `SCRAPER_TRIGGER_KEY` (her biri ≥32 karakter) doldurun. İsteğe bağlı `GROQ_API_KEY`. Ayrıntı: [docs/scraper.md](docs/scraper.md).

**3. API** (8080):

```powershell
cd api
.\mvnw.cmd spring-boot:run
```

**4. Scraper** (8081; canlı otobüs/konaklama/yemek için gerekli):

```powershell
cd scraper
.venv\Scripts\uvicorn app.main:app --host 127.0.0.1 --port 8081
```

(İlk kurulum: aşağıdaki scraper `venv` adımları.)

**5. Arayüz** (5173):

```powershell
cd frontend
npm install
npm run dev
```

Scraper (Python + Scrapling, API çalışırken):

```powershell
cd scraper
py -3.12 -m venv .venv
.venv\Scripts\pip install -e .[dev]
.venv\Scripts\scrapling install
.venv\Scripts\patchright install chromium

.venv\Scripts\python -m app.jobs.run --job fuel --dry-run   # API'ye göndermeden dene
.venv\Scripts\python -m app.jobs.run --job bus              # bus | fuel | targets | all
.venv\Scripts\python -m pytest
```

Scraper HTTP süreci yukarıdaki `uvicorn` komutuyla 8081’de çalışır; zamanlayıcı varsayılan açıktır (`SCRAPER_SCHEDULER_ENABLED=false` ile kapatılır). Rotalar ve cron: `scraper/config/settings.yaml`. Docker imajı: `scraper/Dockerfile`.

Önizlemede otobüs seçilince API, scraper `POST /routes/bus` ile gidiş ve dönüş tarihlerini çeker. `vacation.route-quote-max-age` süresinden yeni bir `route_quotes` kaydı varsa yeniden çekmez. Konaklama için benzer önbellek `lodging-quote-max-age` ile `lodging_offers` tablosunda tutulur. Scraper kapalıysa veya sefer bulunamazsa ulaşım katalog tahminine düşer.

| Job | Kaynak | Varsayılan zaman | API'ye yazılan |
|-----|--------|------------------|----------------|
| `bus` | Otobüs sefer bandı | Pazartesi 03:00 | `ROUTE` gözlemi → `route_quotes` |
| `fuel` | Şehir yakıt tablosu | Ayın 1'i 04:00 | `NATIONAL_FUEL` gözlemi → `fuel_prices` |
| `food` | OpenStreetMap restoran listesi | Önizlemede, istenen il | Liste `destinations` kaydına yazılır. Tutar katalog bandında kalır |
| `targets` | `GET /api/v1/scraper/targets` (JSON_MIN_MAX) | Her gün 05:00 | `POST /api/v1/scraper/ingest` |

`targets` job'ı yalnızca veritabanındaki `scrape_targets` satırları için çalışır; hedefler uygulama açılışında otomatik eklenmez.

## Yapılandırma (`vacation.*`)

`application.yml` ve ortam değişkenleri `VacationProperties` (`api/.../config/VacationProperties.java`) ile bağlanır.

| Anahtar | Varsayılan | Açıklama |
|---------|------------|----------|
| `save-price-snapshots` | `false` | `true` ise kayıtlı seyahatlerde bütçe kalemleri `price_records` tablosuna da yazılır |
| `scraper-api-key` | `${SCRAPER_API_KEY:}` | Scraper → API: `/api/v1/scraper/**` için beklenen `X-Scraper-Key`. Boş veya 32 karakterden kısa ise bu uçlar 503 |
| `scraper-trigger-key` | `${SCRAPER_TRIGGER_KEY:}` | API → scraper: `/routes/bus`, `/lodging/search` ve `/food/search` çağrılarında gönderilen anahtar. Boşsa scraper çağrılmaz |
| `scraper-base-url` | `http://127.0.0.1:8081` | Otobüs ve konaklama HTTP istemcisi |
| `scraper-read-timeout` | `210s` | Scraper cevap bekleme süresi |
| `cors.allowed-origins` | `${CORS_ALLOWED_ORIGINS:...}` | Tarayıcıdan çağırabilecek kaynaklar |
| `route-quote-max-age` | `12h` | Otobüs fiyatı önbelleği |
| `lodging-quote-max-age` | `12h` | Konaklama fiyatı önbelleği |
| `food-quote-max-age` | `7d` | OpenStreetMap restoran listesinin önbelleği |
| `car-fuel-max-age` | `14d` | Araç bütçesinin okuduğu benzin/dizel fiyatının tazelik süresi |
| `car-fuel-round-trip-liters-min` | `25` | Tek araç gidiş-dönüş litre bandının alt ucu |
| `car-fuel-round-trip-liters-max` | `55` | Tek araç gidiş-dönüş litre bandının üst ucu |
| `groq.base-url` | Groq OpenAI uyumlu API | Etkinlik önerisi |
| `groq.model` | `openai/gpt-oss-20b` | Chat modeli |
| `groq.api-key` | `${GROQ_API_KEY:}` | Boşsa `POST .../activity-suggestions` 503 döner |

## API özeti

Akış: [docs/sistem.md](docs/sistem.md).

| Method | Path | Açıklama |
|--------|------|----------|
| POST | `/api/v1/trips/preview` | Bütçe + günlük program + optimizasyon önerileri |
| POST | `/api/v1/trips` | Seyahat kaydet |
| GET | `/api/v1/trips?page=&size=` | Sayfalanmış liste (`PageResponse`: `content`, `page`, `totalPages`, …) |
| GET | `/api/v1/trips/{id}` | Detay + bütçe |
| PUT | `/api/v1/trips/{id}` | Güncelle + bütçe yenile |
| POST | `/api/v1/trips/{id}/refresh-budget` | Bütçeyi yeniden hesapla |
| DELETE | `/api/v1/trips/{id}` | Sil |
| GET | `/api/v1/destinations` | Destinasyonlar |
| GET | `/api/v1/destinations/{cityKey}` | Tek destinasyon |
| GET | `/api/v1/destinations/{cityKey}/activities` | Katalog aktiviteleri |
| POST | `/api/v1/destinations/{cityKey}/activity-suggestions` | Groq ile ek etkinlik fikirleri (JSON dizi) |

`POST /api/v1/trips/preview` yanıtında `itinerary` (günlük öğeler) ve `budgetSuggestions` (tasarruf fikirleri) alanları vardır.

Scraper (API anahtarı gerekir): `GET /api/v1/scraper/targets`, `POST /api/v1/scraper/observations`, `POST /api/v1/scraper/ingest`.

## Önemli bileşenler (API)

### `VacationProperties`

Spring `@ConfigurationProperties(prefix = "vacation")` sınıfı. Scraper adresi ve API anahtarı, fiyat önbellek süreleri, isteğe bağlı fiyat snapshot'ları ve iç içe `Groq` ayarlarını tek yerde toplar. `BusRouteFareService`, `LodgingOfferService`, `ScraperController`, `PriceSnapshotService`, `GroqClientConfig` ve `ActivitySuggestionServiceImpl` bu bean'i kullanır.

### `ActivitySuggestionService` / `ActivitySuggestionServiceImpl`

Arayüz: `suggest(cityKey, ActivitySuggestionRequest)` → `List<SuggestedActivityDto>`.

Uygulama, `prompts/activity-suggest.txt` sistem prompt'unu okur; destinasyon görünen adını ve katalogdaki mevcut aktivite adlarını kullanıcı mesajına ekler; Groq `chat/completions` çağrısı yapar. Yanıt `ActivitySuggestionParser` ile doğrulanır. `DestinationController` üzerinden `POST /api/v1/destinations/{cityKey}/activity-suggestions` ile sunulur; öneriler veritabanına yazılmaz, yalnızca istemciye döner.

### `ActivitySuggestionParser`

Groq çıktısı her zaman saf JSON olmayabilir; parser metin içinde ilk `[` … son `]` aralığını alır ve JSON dizisi olarak okur. Her öğede yalnızca `name` ve `description` alanlarına izin verilir; boş `name` atlanır; en fazla 6 öğe döner. Biçim veya alan hatasında `IllegalArgumentException` fırlatır (servis katmanı bunu 502'ye çevirir).

### `ItineraryService` / `ItineraryServiceImpl`

Arayüz: `build(TripRequest, aktiviteler, seçili aktivite id'leri)` → `List<DayPlanDto>`.

`TripPreviewServiceImpl` bütçe hesabından sonra çağırır. Seçili katalog aktiviteleri ve `customActivityNames` sırayla listelenir; her gün için sabit öğünler (kahvaltı, öğle, akşam), ilk günde “Varış ve yerleşme”, gün sayısına göre en fazla bir aktivite/gün yerleştirilir; fazla aktiviteler son güne eklenir. Bu bir taslak programdır; saat ve rota optimizasyonu yapmaz.

### Katalog

Açılışta örnek şehir yazılmaz. `destinations` tablosunda olmayan şehir için önizleme, aktivite listesi ve tekil destinasyon 404 döner; uydurma fiyat bandı yoktur. Şehir adı 81 il slug’ına `CityKeyNormalizer` ile iner (`api/src/main/resources/tr_provinces.json`).

Akış: [docs/sistem.md](docs/sistem.md).

## Arayüz

Thymeleaf ve htmx kullanılmaz. Tarayıcı arayüzü `frontend/` altında ayrı bir Vue 3 + Vite uygulamasıdır. Spring Boot yalnızca JSON sunar. Sayfalar mevcut `/api/v1` uçlarını çağırır; kimlik ve yetki kuralları değişmez.

| Sayfa | Yol | API |
|-------|-----|-----|
| Giriş | `/login` | `POST /api/v1/auth/login` |
| Kayıt | `/register` | `POST /api/v1/auth/register` |
| Ana sayfa | `/` | `GET /api/v1/destinations`, `GET /api/v1/trips` |
| Fiyatlar | `/pricing` | destinasyonlar, önizleme, kayıt, Groq öneri |
| Seyahatlerim | `/trips`, `/trips/:id` | sayfalı liste, detay, güncelleme, önizleme |
| Profil | `/profile` | `GET /api/v1/auth/me`, `PUT /api/v1/auth/profile` |

Yönetici listeleri arayüzde yok. `GET /api/v1/admin/users` ve `GET /api/v1/admin/trips` durur; `ADMIN` rolü ister.

### Teknolojiler

Vue 3 (Composition API), Vue Router, Vite, Tailwind CSS 4. HTTP için tarayıcı `fetch` kullanılır. Oturum durumu `frontend/src/stores/auth.js` içindeki composable’dadır. Pinia ve Axios yoktur.

### Kurulum ve geliştirme

API’nin ayakta olması gerekir (yukarıdaki `spring-boot:run` veya eşdeğeri).

```powershell
cd frontend
npm install
npm run dev
```

Arayüz `http://localhost:5173` adresindedir. Vite, `/api` isteklerini `API_PROXY_TARGET` (varsayılan `http://127.0.0.1:8080`) adresine iletir. Tarayıcı API’yi aynı kökenden gördüğü için access ve refresh çerezleri (`SameSite=Lax`, HttpOnly) çalışır.

Üretim önizlemesi, aynı vekil ile:

```powershell
cd frontend
npm run build
npm run preview
```

`npm run build` çıktısı `frontend/dist` klasörüdür. Statik dosyaları yayınlarken `/api` yolunu 8080’deki API’ye vekil edin ve `VITE_API_BASE_URL` boş kalsın. Ayrı bir kökene mutlak API adresi yazmak, mevcut çerez oturumunu tarayıcıya taşımaz.

### Ortam değişkenleri

`frontend/.env.example` kopyalanabilir. Geliştirme değerleri `frontend/.env.development` içindedir.

| Değişken | Nerede | Anlamı |
|----------|--------|--------|
| `VITE_API_BASE_URL` | istemci | Boşsa istekler sayfanın köküne gider. Doluysa bu kökün sonuna `/api/v1/...` eklenir |
| `API_PROXY_TARGET` | yalnız Vite | `npm run dev` ve `npm run preview` sırasında `/api` hedefi |

### Backend bağlantısı

Oturum çerezdedir; token gövdesi okunmaz. `POST` istekleri önce `GET /api/v1/auth/me` ile `XSRF-TOKEN` çerezini alır, sonra `X-CSRF-TOKEN` başlığını gönderir. `credentials: 'include'` açıktır. `401` (giriş, kayıt ve `/me` dışında) yerel oturumu siler ve fiyat sayfasındaysa girişe döner.

Giriş `POST /api/v1/auth/login` gövdesi `{ "email", "password" }` şeklindedir. Başarıda kullanıcı JSON’u ve çerezler gelir; arayüz `/pricing` sayfasına gider. Kayıt `POST /api/v1/auth/register` (parola 8–72 karakter). API oturum çerezi yazar; arayüz hemen `POST /api/v1/auth/logout` çağırır ve giriş sayfasına döner. Çıkış aynı logout ucudur.

Fiyat sayfası il listesini `GET /api/v1/destinations` ile doldurur. Seçilen güzergâh, tarih ve kişi sayısı `POST /api/v1/trips/preview` gövdesine gider; yanıtaki `budget` toplam, kişi başı, günlük ve satırları çizer. `GET /api/v1/trips?page=&size=` kayıtlı seyahat bütçelerini sayfalar (`content` dizisi). Önizleme ve seyahat listesi oturum ister. Hata gövdesi `{ "code", "message", "fields" }` olarak gösterilir.

## Modüller

| Modül | Açıklama |
|-------|----------|
| `api` | Spring Boot: REST, PostgreSQL |
| `frontend` | Vue 3 + Vite |
| `scraper` | Python batch scraper (Scrapling + APScheduler + FastAPI) |

## Mimari

`controller` → `service` (+ `impl`) → `repository` → `entity`  
`dto` (JSON), `model` (hesaplama), `exception`, `config`

## Sonraki adımlar

- Obilet otel sayfası karta gecelik fiyat yazmıyor; önizleme katalog tahminini kullanır
- Parola sıfırlama ve admin ekranı bu planda yok
- Tarayıcıda uçtan uca gezinme otomasyonu yok; Postgres şema testi `PostgresSchemaIT`
