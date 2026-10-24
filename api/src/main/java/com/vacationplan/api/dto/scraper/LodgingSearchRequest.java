package com.vacationplan.api.dto.scraper;

import java.time.LocalDate;

import com.vacationplan.api.model.AccommodationType;

public record LodgingSearchRequest(
		String city,
		LocalDate checkIn,
		LocalDate checkOut,
		int adults,
		AccommodationType accommodationType
) {
}
