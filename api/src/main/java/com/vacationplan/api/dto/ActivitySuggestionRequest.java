package com.vacationplan.api.dto;

import java.time.LocalDate;

import com.vacationplan.api.model.TravelStyle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ActivitySuggestionRequest(
		LocalDate startDate,
		LocalDate endDate,
		@Min(1) @Max(20) Integer adults,
		TravelStyle travelStyle
) {
}
