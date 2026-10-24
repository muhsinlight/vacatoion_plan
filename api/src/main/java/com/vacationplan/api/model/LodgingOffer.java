package com.vacationplan.api.model;

import java.math.BigDecimal;
import java.util.List;

public record LodgingOffer(
		LodgingTier tier,
		String provider,
		String name,
		Integer stars,
		BigDecimal rating,
		String boardType,
		List<String> features,
		BigDecimal pricePerNight,
		String url,
		String imageUrl
) {
}
