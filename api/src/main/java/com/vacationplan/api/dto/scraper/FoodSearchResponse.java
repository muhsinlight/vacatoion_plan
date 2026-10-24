package com.vacationplan.api.dto.scraper;

import java.util.List;

public record FoodSearchResponse(
		String cityKey,
		String provider,
		int saved,
		FoodBandDto band,
		List<FoodPlaceDto> places,
		List<String> errors
) {
}
