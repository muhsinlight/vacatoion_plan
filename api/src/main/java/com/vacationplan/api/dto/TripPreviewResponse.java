package com.vacationplan.api.dto;

import java.util.List;

import com.vacationplan.api.dto.scraper.FoodPlaceDto;

public record TripPreviewResponse(
		List<Assumption> assumptions,
		BudgetPreviewDto budget,
		List<DayPlanDto> itinerary,
		List<BudgetSuggestionDto> budgetSuggestions,
		String priceNote,
		LodgingPreviewDto lodging,
		List<FoodPlaceDto> foodPlaces,
		List<BusLegDto> busLegs
) {
}
