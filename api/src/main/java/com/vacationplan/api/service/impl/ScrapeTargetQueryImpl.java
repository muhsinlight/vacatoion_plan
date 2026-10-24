package com.vacationplan.api.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.vacationplan.api.dto.scraper.ScrapeTargetDto;
import com.vacationplan.api.repository.ScrapeTargetRepository;
import com.vacationplan.api.service.ScrapeTargetQuery;

@Service
public class ScrapeTargetQueryImpl implements ScrapeTargetQuery {

	private final ScrapeTargetRepository scrapeTargetRepository;

	public ScrapeTargetQueryImpl(ScrapeTargetRepository scrapeTargetRepository) {
		this.scrapeTargetRepository = scrapeTargetRepository;
	}

	@Override
	public List<ScrapeTargetDto> listEnabled() {
		return scrapeTargetRepository.findByEnabledTrueOrderByCityKeyAscIdAsc()
				.stream()
				.filter(target -> target.getCatalogField().acceptsScrape())
				.map(ScrapeTargetDto::from)
				.toList();
	}
}
