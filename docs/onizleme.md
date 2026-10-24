# Önizleme ve kayıt

Harita [sistem.md](sistem.md) içindedir. `POST /api/v1/trips/preview` kaydetmez. Aynı hesap `POST /api/v1/trips` ile `trips` ve `trip_budget_lines` tablolarına yazılır.

## İstek

Zorunlu alanlar: `originCity`, `destinationCity`, `startDate`, `endDate`. `adults` boşsa 1 olur, en fazla 20. `includeContingency` boşsa true olur. `customActivityNames` kırpılır, tekrarı atılır, en fazla 8 ad ve ad başına 80 karakter kalır.

Boş bırakılınca `TripPreviewServiceImpl` şunları varsayar ve `assumptions` listesine yazar:

| Alan | Varsayılan |
|------|------------|
| `endDate` gidişten önceyse | Gidiş + 2 gün |
| `transportMode` | `BUS` |
| `accommodationType` | Aynı günse `NONE`, değilse `HOTEL` |
| `travelStyle` | `BALANCED` |

İl adı `CityKeyNormalizer` ile 81 il slug’ına iner (`api/src/main/resources/tr_provinces.json`). `destinations` satırı yoksa 404 döner. Uydurma fiyat bandı yoktur.

## Hesabın sırası

1. `TripController.preview` gövdeyi doğrular.
2. `TripPreviewServiceImpl.plan` varsayılanları uygular.
3. `CatalogServiceImpl.resolveForTrip` satırdan `DestinationProfile` üretir.
4. Ulaşım `BUS` ise `BusRouteFareServiceImpl` önce `route_quotes` önbelleğine bakar (`vacation.route-quote-max-age`, varsayılan 12 saat). Kayıt yoksa veya eskimişse scraper `POST /routes/bus` çağrılır. Scraper kapalıysa, kota doluysa veya sefer yoksa katalog bandına düşülür. Ulaşım `CAR` ise `CarFuelQuoteServiceImpl` `fuel_prices` içindeki son benzin ve dizel fiyatını okur (`vacation.car-fuel-max-age`, varsayılan 14 gün). Tutar tek araç için `car-fuel-round-trip-liters-min`–`max` (varsayılan 25–55 L) × litre fiyatıdır. Fiyat yoksa veya eskimişse katalog araç bandı, kişi sayısıyla çarpılmadan yazılır.
5. Konaklama otel, pansiyon veya kiralıksa `LodgingOfferServiceImpl` aynı yaş kuralıyla `lodging_offers` okur (`vacation.lodging-quote-max-age`). Gerekirse scraper `POST /lodging/search`.
6. Yeme-içme `FoodQuoteServiceImpl` ile bakılır. `food_source_type` resmi ve `food_checked_at` `vacation.food-quote-max-age` (varsayılan 7 gün) içindeyse katalog bandı kullanılır. Değilse scraper `POST /food/search`. Liste OpenStreetMap restoran adlarıdır; öğün fiyatı üretmez, tutar katalog tahmininde kalır. Liste boşsa 15 dakika boyunca aynı il yeniden sorulmaz.
7. `BudgetCalculationServiceImpl` kalemleri üretir: `TRANSPORT`, `LODGING`, `FOOD`, `ACTIVITY`, `LOCAL_TRANSPORT`, `SHOPPING`, `OTHER`.
8. `ItineraryServiceImpl` taslak program kurar. Her günde kahvaltı, öğle ve akşam vardır. İlk günde varış vardır. Seçili katalog etkinliği ve `customActivityNames` sırayla dağılır; günde en fazla bir etkinlik, taşanlar son gündedir. Saat ve rota optimizasyonu yoktur.
9. `BudgetOptimizerServiceImpl` `budgetSuggestions` üretir.
10. `TripPlanResult`, `TripMapper` ile `TripPreviewResponse` olur.

`PUT /api/v1/trips/{id}` ve `POST /api/v1/trips/{id}/refresh-budget` aynı planı yeniden hesaplar. `vacation.save-price-snapshots` true ise kalemler `price_records` tablosuna da yazılır. Varsayılan false’tur.

## Destinasyon ve Groq

| Method | Path | Ne olur |
|--------|------|---------|
| GET | `/api/v1/destinations` | İl anahtarı ve görünen ad |
| GET | `/api/v1/destinations/{cityKey}` | Tek destinasyon |
| GET | `/api/v1/destinations/{cityKey}/activities` | Katalog etkinlikleri |
| POST | `/api/v1/destinations/{cityKey}/activity-suggestions` | Groq önerisi; veritabanına yazılmaz |

Öneri yolu `prompts/activity-suggest.txt` dosyasını okur, katalogdaki adları mesaja ekler. `ActivitySuggestionParser` metindeki ilk `[` … son `]` aralığını dizi diye okur. Yalnızca `name` ve `description` kalır, en fazla 6 öğe döner. `GROQ_API_KEY` boşsa 503 döner.

Saatlik hak `request_quota_windows` tablosundadır. Groq kovası saatte 30, scraper çağrısı saatte 20 (`vacation.quota`). Dolunca 429 döner.

## HTTP hataları

Gövde `ApiErrorResponse` olur. Karşılayan sınıf `GlobalExceptionHandler`.

| Durum | Ne zaman |
|-------|----------|
| 400 | Doğrulama, `VacationException`, `IllegalArgumentException` |
| 404 | Bilinmeyen seyahat veya destinasyon, eşleşmeyen yol |
| 429 | Kota doldu |
| 502 | Groq yanıtı okunamadı veya upstream başarısız |
| 503 | Groq anahtarı yok |
| 500 | Beklenmeyen hata. Ayrıntı loga yazılır, istemciye genel mesaj gider. |
