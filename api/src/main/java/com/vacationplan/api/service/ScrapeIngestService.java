package com.vacationplan.api.service;

import com.vacationplan.api.dto.scraper.ScrapeIngestRequest;
import com.vacationplan.api.dto.scraper.ScrapeIngestResponse;

/**
 * Hedef bazlı scrape sonucunun kataloğa işlenmesi.
 */
public interface ScrapeIngestService {

	/**
	 * Tek hedefin scrape sonucunu kaydeder.
	 *
	 * @param request scrape sonucu
	 * @return işleme özeti
	 */
	ScrapeIngestResponse ingest(ScrapeIngestRequest request);
}
