package com.vacationplan.api.exception;

public class ScrapeException extends VacationException {

	private ScrapeException(String code, String message) {
		super(code, message);
	}

	public static ScrapeException targetNotFound(Long id) {
		return new ScrapeException("SCRAPE_TARGET_NOT_FOUND", "Scrape target bulunamadı: " + id);
	}

	public static ScrapeException targetDisabled(Long id) {
		return new ScrapeException("SCRAPE_TARGET_DISABLED", "Scrape target devre dışı: " + id);
	}

	public static ScrapeException lodgingNotViaCatalog(Long id) {
		return new ScrapeException(
				"SCRAPE_LODGING_NOT_VIA_CATALOG",
				"Konaklama fiyatı katalog hedefiyle yazılmaz; lodging_offers kullanılır: " + id);
	}

	public static ScrapeException minExceedsMax() {
		return new ScrapeException("SCRAPE_MIN_EXCEEDS_MAX", "min max değerinden büyük olamaz");
	}

	public static ScrapeException minExceedsMax(String scopeKey) {
		return new ScrapeException("SCRAPE_MIN_EXCEEDS_MAX", "min max değerinden büyük: " + scopeKey);
	}

	public static ScrapeException activityKeyRequired() {
		return new ScrapeException("SCRAPE_ACTIVITY_KEY_REQUIRED", "ACTIVITY_ENTRY için activityKey gerekli");
	}

	public static ScrapeException activityNotFound(String activityKey) {
		return new ScrapeException("SCRAPE_ACTIVITY_NOT_FOUND", "Aktivite bulunamadı: " + activityKey);
	}

	public static ScrapeException destinationNotFound(String cityKey) {
		return new ScrapeException("SCRAPE_DESTINATION_NOT_FOUND", "Destinasyon bulunamadı: " + cityKey);
	}

	public static ScrapeException routeKeysRequired() {
		return new ScrapeException(
				"SCRAPE_ROUTE_KEYS_REQUIRED",
				"ROUTE gözlemi için originKey, destKey, transportMode ve weekBucket gerekli");
	}

	public static ScrapeException trainQuoteUnsupported() {
		return new ScrapeException("SCRAPE_TRAIN_UNSUPPORTED", "TRAIN route quote desteklenmiyor");
	}

	public static ScrapeException negativeAmount(String scopeKey) {
		return new ScrapeException("SCRAPE_NEGATIVE_AMOUNT", "tutar negatif olamaz: " + scopeKey);
	}

	public static ScrapeException typicalOutOfBand(String scopeKey) {
		return new ScrapeException("SCRAPE_TYPICAL_OUT_OF_BAND", "typical min-max aralığında olmalı: " + scopeKey);
	}

	public static ScrapeException invalidFuelScope(String scopeKey) {
		return new ScrapeException("SCRAPE_INVALID_FUEL", "Geçersiz yakıt scope_key: " + scopeKey);
	}
}
