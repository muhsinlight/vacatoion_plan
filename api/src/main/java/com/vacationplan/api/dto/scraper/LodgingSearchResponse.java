package com.vacationplan.api.dto.scraper;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vacationplan.api.model.LodgingTier;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LodgingSearchResponse(
		String provider,
		LodgingMoneyBand band,
		List<ScraperLodgingOffer> offers,
		List<String> errors,
		Boolean estimated
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record LodgingMoneyBand(
			BigDecimal min,
			BigDecimal typical,
			BigDecimal max,
			String currency,
			Integer sampleSize
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ScraperLodgingOffer(
			LodgingTier tier,
			String provider,
			String name,
			Integer stars,
			BigDecimal rating,
			String boardType,
			List<String> features,
			BigDecimal pricePerNight,
			BigDecimal totalPrice,
			String url,
			String imageUrl
	) {
	}
}
