package com.vacationplan.api.service;

/**
 * Şehir yemek bandı eskimişse scraper'dan Google Places sonucunu ister.
 */
public interface FoodQuoteService {

	enum Result {
		FRESH,
		UPDATED,
		SKIPPED,
		FAILED,
		/** Önbellek eski. Liste bu yanıtın bekleme süresinde gelmedi. */
		SCHEDULED
	}

	/**
	 * {@code cityKey} il anahtarıdır. Taze liste varsa çağrı yapılmaz.
	 * Eskiyse scraper bitene kadar beklenir; süre dolarsa {@link Result#SCHEDULED}.
	 *
	 * @param cityKey normalize edilmiş il
	 * @return önizlemenin kullanacağı sonuç
	 */
	Result refreshIfStale(String cityKey);
}
