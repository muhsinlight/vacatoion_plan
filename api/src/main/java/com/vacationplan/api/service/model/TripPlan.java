package com.vacationplan.api.service.model;

import java.math.BigDecimal;
import java.util.List;

import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.TransportMode;
import com.vacationplan.api.model.TravelStyle;

/**
 * Plan motorunun okuduğu seyahat. Sahip, ilgi ve özel gereksinim bu kayıtta yoktur.
 */
public record TripPlan(
		String originCity,
		String destinationCity,
		TripStay stay,
		int adults,
		List<Integer> childAges,
		int travelerCount,
		TransportMode transportMode,
		AccommodationType accommodationType,
		TravelStyle travelStyle,
		BigDecimal totalBudget,
		BigDecimal perPersonBudget,
		boolean includeContingency,
		List<String> selectedActivityIds,
		List<String> customActivityNames
) {

	public int nightCount() {
		return stay.nightCount();
	}

	public int dayCount() {
		return stay.dayCount();
	}
}
