package com.vacationplan.api.dto.scraper;

import java.math.BigDecimal;

public record FoodBandDto(
		BigDecimal min,
		BigDecimal typical,
		BigDecimal max,
		String currency,
		Integer sampleSize
) {
}
