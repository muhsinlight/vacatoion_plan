package com.vacationplan.api.dto;

import java.time.LocalDate;

import com.vacationplan.api.model.TripStatus;

public record TripSummaryResponse(
		Long id,
		TripStatus status,
		String originCity,
		String destinationCity,
		LocalDate startDate,
		LocalDate endDate,
		BudgetPreviewDto budget
) {
}
