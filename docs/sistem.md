# Sistem

Çalıştırma komutları ve `vacation.*` anahtarları kök [README.md](../README.md) içindedir. Arayüz ayrı açılır.

| Süreç | Port | Kod |
|-------|------|-----|
| PostgreSQL | 5433 | `api/compose.yaml` |
| API | 8080 | `api/src/main/java/com/vacationplan/api` |
| Scraper | 8081 | `scraper/app` |
| Arayüz | 5173 | `frontend` |

API 8080 yalnızca JSON sunar. Thymeleaf yoktur. Tarayıcı arayüzü ayrı Vue sürecidir (`frontend`, geliştirmede 5173).

Katman sırası: `controller` isteği alır, `service` sözleşmeyi söyler, `service.impl` işi yapar, `repository` satırı okur veya yazar. JSON `dto` paketindedir. Hesap ara tipi `model` paketindedir. JPA satırı `entity` paketindedir ve HTTP gövdesi olmaz.

Kullanıcı akışı: [onizleme.md](onizleme.md). Scraper akışı: [scraper.md](scraper.md).

## Paketler

Fikrin tam cümlesi kök `com.vacationplan.api` paketindeki `package-info.java` dosyasındadır. `controller`, `dto` ve `exception` kendi özetini tutar. Metot sözleşmesi sınıfın üstündeki Javadoc’tadır. Alan listesi Swagger’dadır (`/swagger-ui.html`).

| Paket | Fikir |
|-------|--------|
| `controller` | Seyahat, destinasyon ve scraper giriş uçları. Hesap yapmaz. |
| `service` | İş sözleşmesi. Önizleme kaydetmez. |
| `service.impl` | Sözleşmenin Spring uygulaması. Önizleme burada birleşir. |
| `service.model` | Servisler arası hesap sonucu. JSON değildir. |
| `dto` | İstemcinin gördüğü kayıtlar. |
| `dto.mapper` | Hesap modeli ile JSON arasındaki çeviri. |
| `dto.scraper` | Scraper batch gövdeleri. Sayfalar bunları çağırmaz. |
| `entity` | Tablolar. |
| `repository` | Spring Data. Fiyat çözmez. |
| `model` | Para aralığı, ulaşım, çözülmüş fiyat. |
| `model.catalog` | İl profili: bantlar ve katalog etkinlikleri. |
| `config` | `vacation.*`, CORS, anahtar filtresi, HTTP istemcileri. |
| `exception` | Alan hatası ve tek `RestControllerAdvice`. |
| `util` | İl slug’ı, anahtar biçimi, otobüs hafta kovası. |

Kayıt, giriş, refresh ve `GET /api/v1/destinations/**` (il listesi, il ve aktiviteler) anonimdir. `POST .../activity-suggestions` oturum ister. Diğer `/api/v1/**` uçları oturum ister. `/api/v1/admin/**` `ADMIN` rolü ister. Sağlık açıktır. Swagger `prod` profilinde kapalıdır. Bunun dışındaki yollar reddedilir. Oturum yoktur; kimlik JWT çerezidir. `/api/v1/scraper/**` ayrıca `X-Scraper-Key` ister.

## Tablolar

| Tablo | Kim yazar | Kim okur |
|-------|-----------|----------|
| `destinations`, `catalog_activities` | Katalog verisi | Önizleme, destinasyon uçları |
| `trips`, `trip_budget_lines` | `TripServiceImpl` | Seyahat uçları |
| `route_quotes` | Rota gözlemi veya otobüs araması | `BusRouteFareServiceImpl` |
| `lodging_offers` | Konaklama araması | `LodgingOfferServiceImpl` |
| `price_observations` | `POST /api/v1/scraper/observations` | Gözlem kaydı |
| `fuel_prices` | Yakıt gözlemi | Araç seçilince son benzin ve dizel fiyatı |
| `price_records` | `vacation.save-price-snapshots` açıksa seyahat kaydı | — |
| `scrape_targets` | Veritabanı; açılışta satır eklenmez | `targets` işi |
| `request_quota_windows` | `RequestQuotaServiceImpl` | Aynı servis |
