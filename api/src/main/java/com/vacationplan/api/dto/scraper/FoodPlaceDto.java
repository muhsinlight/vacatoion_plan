package com.vacationplan.api.dto.scraper;

import java.math.BigDecimal;

public record FoodPlaceDto(
		String name,
		BigDecimal rating,
		Integer reviewCount,
		BigDecimal mealMin,
		BigDecimal mealMax,
		String url,
		Boolean michelin
) {
}
