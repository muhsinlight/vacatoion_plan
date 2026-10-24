package com.vacationplan.api.service.impl;

import org.springframework.stereotype.Component;

import com.vacationplan.api.dto.scraper.PriceObservationDto;
import com.vacationplan.api.entity.PriceObservation;
import com.vacationplan.api.entity.RouteQuote;
import com.vacationplan.api.exception.ScrapeException;
import com.vacationplan.api.model.PriceScopeType;
import com.vacationplan.api.model.TransportMode;
import com.vacationplan.api.repository.RouteQuoteRepository;
import com.vacationplan.api.util.CityKeyNormalizer;

@Component
public class RouteQuoteMaterializer {

	private final RouteQuoteRepository routeQuoteRepository;

	public RouteQuoteMaterializer(RouteQuoteRepository routeQuoteRepository) {
		this.routeQuoteRepository = routeQuoteRepository;
	}

	public boolean materialize(PriceObservationDto dto, PriceObservation saved) {
		if (dto.scopeType() != PriceScopeType.ROUTE) {
			return false;
		}
		if (dto.originKey() == null || dto.destKey() == null || dto.transportMode() == null || dto.weekBucket() == null) {
			throw ScrapeException.routeKeysRequired();
		}
		if (dto.transportMode() == TransportMode.TRAIN) {
			throw ScrapeException.trainQuoteUnsupported();
		}

		String origin = CityKeyNormalizer.normalize(dto.originKey());
		String dest = CityKeyNormalizer.normalize(dto.destKey());

		RouteQuote quote = routeQuoteRepository
				.findByOriginKeyAndDestKeyAndTransportModeAndWeekBucket(
						origin, dest, dto.transportMode(), dto.weekBucket())
				.orElseGet(RouteQuote::new);

		quote.setOriginKey(origin);
		quote.setDestKey(dest);
		quote.setTransportMode(dto.transportMode());
		quote.setWeekBucket(dto.weekBucket());
		quote.setMinAmount(dto.min());
		quote.setTypicalAmount(dto.typical());
		quote.setMaxAmount(dto.max());
		quote.setCurrency(dto.currency());
		quote.setObservation(saved);
		quote.setCheckedAt(saved.getCheckedAt());
		routeQuoteRepository.save(quote);
		return true;
	}
}
