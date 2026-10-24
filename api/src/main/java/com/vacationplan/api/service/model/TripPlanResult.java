package com.vacationplan.api.service.model;

import java.util.List;

import com.vacationplan.api.dto.Assumption;
import com.vacationplan.api.dto.DayPlanDto;
import com.vacationplan.api.dto.scraper.FoodPlaceDto;
import com.vacationplan.api.model.BudgetBreakdown;
import com.vacationplan.api.model.BudgetSuggestion;
import com.vacationplan.api.model.ResolvedBusFare;
import com.vacationplan.api.model.ResolvedLodging;

public record TripPlanResult(
		TripPlan plan,
		List<Assumption> assumptions,
		BudgetBreakdown budget,
		List<DayPlanDto> itinerary,
		List<BudgetSuggestion> budgetSuggestions,
		String priceNote,
		ResolvedLodging lodging,
		List<FoodPlaceDto> foodPlaces,
		ResolvedBusFare busFare
) {
}
