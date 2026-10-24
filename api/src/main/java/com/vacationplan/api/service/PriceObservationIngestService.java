package com.vacationplan.api.service;

import com.vacationplan.api.dto.scraper.PriceObservationBatchRequest;
import com.vacationplan.api.dto.scraper.PriceObservationBatchResponse;

/**
 * Scraper fiyat gözlemlerini kalıcı kayda çevirir.
 */
public interface PriceObservationIngestService {

	/**
	 * Gözlem grubunu yazar.
	 *
	 * @param request gözlem grubu
	 * @return yazılan kayıt ve hata özeti
	 */
	PriceObservationBatchResponse ingestBatch(PriceObservationBatchRequest request);
}
