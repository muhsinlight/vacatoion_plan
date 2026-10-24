---
name: source-map
description: >-
  Vacation Plan kaynak haritası. Kod aramadan önce bunu oku. Dosya adı
  tablodaysa yalnız o yolu aç. Kökte, api/, scraper/ veya frontend/ altında
  glob veya grep yapma. Bir turda en fazla 4 dosya.
---

# Kaynak haritası

Kök `README.md` çalıştırma ve `vacation.*` anahtarlarıdır. Sistem özeti `docs/sistem.md`, scraper akışı `docs/scraper.md`, kullanıcı akışı `docs/onizleme.md`.

Dosya adı aşağıdaki tablolardaysa yolu doğrudan oku. Değilse yalnızca ilgili tek dizinde, sınıf veya fonksiyon adıyla ara. `scraper/.venv`, `site-packages`, `api/target`, `node_modules`, `.pytest_cache` açma.

## API

Kök `api/src/main/java/com/vacationplan/api`. Testler `api/src/test/java`. Ayar `api/src/main/resources` ve `api/pom.xml`.

Katman: `controller` alır, `service` sözleşmeyi söyler, `service/impl` yapar, `repository` satırı okur. JSON `dto`, hesap ara tipi `service/model` ve `model`, tablo `entity`.

| Uç | Dosya |
|----|--------|
| Kayıt, giriş, oturum | `controller/AuthController.java`, `service/impl/AuthServiceImpl.java`, `security/JwtService.java` |
| Seyahat ve önizleme | `controller/TripController.java`, `service/impl/TripServiceImpl.java`, `service/impl/TripPreviewServiceImpl.java` |
| Bütçe | `service/impl/BudgetCalculationServiceImpl.java`, `service/impl/BudgetOptimizerServiceImpl.java` |
| Destinasyon | `controller/DestinationController.java`, `service/impl/CatalogServiceImpl.java` |
| Scraper girişi | `controller/ScraperController.java`, `service/impl/ScrapeIngestServiceImpl.java`, `service/impl/PriceObservationIngestServiceImpl.java` |
| Otobüs fiyatı | `service/impl/BusRouteFareServiceImpl.java`, `service/impl/RouteQuoteMaterializer.java` |
| Konaklama | `service/impl/LodgingOfferServiceImpl.java` |
| Yönetim | `controller/AdminController.java` |
| Kota | `service/impl/RequestQuotaServiceImpl.java` |

Tablo adları `docs/sistem.md` içindedir. Entity ve repository dosya adı tabloyla aynı köktedir (`Trip.java` / `TripRepository.java`).

## Scraper

Kök `scraper/app`. Test `scraper/tests`. Ayar `scraper/config/settings.yaml` ve `scraper/pyproject.toml`.

| İş | Dosya |
|----|--------|
| HTTP uçları | `main.py` |
| API istemcisi | `vacation_api.py` |
| Zamanlayıcı | `jobs/scheduler.py`, `jobs/run.py` |
| Otobüs, otel, konum | `providers/obilet_bus.py`, `providers/obilet_hotels.py`, `providers/obilet_locations.py` |
| Yemek | `providers/osm_food.py`, `food_search.py` |
| Kiralık, yakıt, hedef sayfası | `providers/airbnb_rentals.py`, `providers/fuel_prices.py`, `providers/scrape_targets.py` |
| Bant ve konaklama hesabı | `bands.py`, `lodging_tiers.py`, `lodging_search.py`, `fetch_budget.py` |

## Arayüz

Vue kaynağı yalnız `frontend/src`. `start.ps1` arayüzü açmaz; `frontend` içinde `npm run dev` (5173).

| Ekran veya iş | Dosya |
|---------------|--------|
| Rotalar | `router/index.js` — `/login`, `/register`, `/`, `/pricing`, `/trips`, `/profile` |
| Giriş, kayıt, ana sayfa, fiyat, seyahat, profil | `views/Login.vue`, `views/Register.vue`, `views/Home.vue`, `views/Pricing.vue`, `views/Trips.vue`, `views/Profile.vue` |
| Oturum ve ad | `stores/auth.js`, `services/auth.js`, `utils/user.js`, `components/UserMenu.vue` |
| API çağrısı | `services/http.js`, `services/pricing.js` |
| Fiyat biçimi | `composables/usePrices.js`, `utils/format.js` |
| Yerleşim | `layouts/AppLayout.vue`, `layouts/AuthLayout.vue` |

## Sözleşme

İstek ve yanıt örnekleri `contract/` altındadır. İl anahtarları `contract/tr_provinces.json`. Scraper gövdeleri `scrape_ingest_request.json`, `price_observation_batch.json`, `lodging_search_request.json`.
