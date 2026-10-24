package com.vacationplan.api.service;

import java.util.List;

import com.vacationplan.api.dto.scraper.ScrapeTargetDto;

/**
 * Kayıtlı kazıma hedeflerini okur. Hedef yazmaz.
 */
public interface ScrapeTargetQuery {

	/**
	 * Etkin ve scrape kabul eden hedefler.
	 *
	 * @return il anahtarına göre sıralı hedefler
	 */
	List<ScrapeTargetDto> listEnabled();
}
