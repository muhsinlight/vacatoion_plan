package com.vacationplan.api.model.catalog;

import java.util.List;

import com.vacationplan.api.dto.scraper.FoodPlaceDto;
import com.vacationplan.api.model.ActivityOption;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.PriceSourceType;

public record DestinationProfile(
		String cityKey,
		String displayName,
		MoneyRange busRoundTripPerAdult,
		MoneyRange trainRoundTripPerAdult,
		MoneyRange planeRoundTripPerAdult,
		MoneyRange carRoundTripPerAdult,
		MoneyRange lodgingPerNight,
		MoneyRange foodPerPersonPerDay,
		PriceSourceType foodSourceType,
		MoneyRange localTransportPerDay,
		List<ActivityOption> activities,
		List<FoodPlaceDto> foodPlaces
) {
	public DestinationProfile {
		if (foodPlaces == null) {
			foodPlaces = List.of();
		}
	}
}
