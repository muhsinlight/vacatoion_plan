/**
 * Tatil planı API'sinin kökü. Süreç {@link com.vacationplan.api.ApiApplication} ile açılır.
 *
 * <p>İstek {@code controller} katmanından girer, sözleşme {@code service} arayüzündedir,
 * uygulama {@code service.impl} içindedir, satır {@code repository} üzerinden {@code entity}
 * tablosuna iner. JSON {@code dto}, hesap ara tipi {@code model} paketindedir.
 * Paket özeti burada toplanır. {@code controller}, {@code dto} ve {@code exception}
 * kendi sayfalarını tutar.
 *
 * <ul>
 *   <li>{@code controller} — seyahat, destinasyon ve scraper giriş uçları. Gövde doğrulanır;
 *       bütçe, program ve fiyat çözülmez.</li>
 *   <li>{@code service} — controller ve testin bağlandığı sözleşme. Önizleme kaydetmez;
 *       kayıtlı seyahat üretilen planı saklar. Fiyat yazan uçlar scraper içindir.</li>
 *   <li>{@code service.impl} — katalog profili, otobüs ve konaklama fiyatı, bütçe, günlük
 *       program ve tasarruf önerisi burada birleşir.</li>
 *   <li>{@code service.model} — servisler arası hesap sonucu. {@code TripPlanResult} JSON değildir;
 *       istemci gövdesini {@code TripMapper} üretir.</li>
 *   <li>{@code dto} — istemcinin gördüğü kayıtlar. Çeviri {@code dto.mapper}, scraper batch
 *       gövdeleri {@code dto.scraper} altındadır. Mobil scraper tiplerini çağırmaz.</li>
 *   <li>{@code entity} — JPA satırları. İstek veya yanıt gövdesi olmaz.</li>
 *   <li>{@code repository} — Spring Data. Fiyat çözmez ve HTTP konuşmaz.</li>
 *   <li>{@code model} — para aralığı, ulaşım, konaklama kademesi, çözülmüş fiyat.
 *       İl profili {@code model.catalog} altındadır.</li>
 *   <li>{@code bootstrap} — süreç açılırken bir kez çalışan katalog tohumu.
 *       İstek yoluna girmez. Eksik ili yazar, var olan fiyatın üzerine yazmaz.</li>
 *   <li>{@code config} — {@code vacation.*}, CORS, saat, OpenAPI ve HTTP istemcileri.
 *       İş kuralı yazılmaz. Scraper uçlarını {@code ScraperKeyFilter} keser.</li>
 *   <li>{@code exception} — alan hataları ve bunları {@code ApiErrorResponse} yapan tek
 *       {@code RestControllerAdvice}.</li>
 *   <li>{@code util} — il anahtarı, scraper anahtar biçimi, otobüs hafta kovası.
 *       İl slug'ı {@code tr_provinces.json} dosyasındadır.</li>
 * </ul>
 */
package com.vacationplan.api;
