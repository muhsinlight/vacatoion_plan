package com.vacationplan.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vacationplan.api.dto.scraper.PriceObservationBatchRequest;
import com.vacationplan.api.dto.scraper.PriceObservationBatchResponse;
import com.vacationplan.api.dto.scraper.ScrapeIngestRequest;
import com.vacationplan.api.dto.scraper.ScrapeIngestResponse;
import com.vacationplan.api.dto.scraper.ScrapeTargetDto;
import com.vacationplan.api.service.PriceObservationIngestService;
import com.vacationplan.api.service.ScrapeIngestService;
import com.vacationplan.api.service.ScrapeTargetQuery;

import jakarta.validation.Valid;

/**
 * Scraper'ın yazdığı giriş uçları. Taban yol {@code /api/v1/scraper}.
 *
 * <p>Anahtar kontrolü {@link com.vacationplan.api.config.ScraperKeyFilter} içindedir.
 */
@RestController
@RequestMapping("/api/v1/scraper")
public class ScraperController {

	private final ScrapeTargetQuery scrapeTargetQuery;
	private final ScrapeIngestService scrapeIngestService;
	private final PriceObservationIngestService priceObservationIngestService;

	public ScraperController(
			ScrapeTargetQuery scrapeTargetQuery,
			ScrapeIngestService scrapeIngestService,
			PriceObservationIngestService priceObservationIngestService
	) {
		this.scrapeTargetQuery = scrapeTargetQuery;
		this.scrapeIngestService = scrapeIngestService;
		this.priceObservationIngestService = priceObservationIngestService;
	}

	/**
	 * Etkin ve scrape kabul eden hedefler.
	 *
	 * @return il anahtarına göre sıralı hedefler
	 */
	@GetMapping("/targets")
	public List<ScrapeTargetDto> listTargets() {
		return scrapeTargetQuery.listEnabled();
	}

	/**
	 * Fiyat gözlemlerini kaydeder.
	 *
	 * @param request gözlem grubu
	 * @return yazılan kayıt ve hata özeti
	 */
	@PostMapping("/observations")
	public PriceObservationBatchResponse ingestObservations(@Valid @RequestBody PriceObservationBatchRequest request) {
		return priceObservationIngestService.ingestBatch(request);
	}

	/**
	 * Hedef bazlı scrape sonucunu kataloğa işler.
	 *
	 * @param request tek hedefin scrape sonucu
	 * @return işleme özeti
	 */
	@PostMapping("/ingest")
	public ScrapeIngestResponse ingest(@Valid @RequestBody ScrapeIngestRequest request) {
		return scrapeIngestService.ingest(request);
	}
}
