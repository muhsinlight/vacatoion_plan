package com.vacationplan.api.dto;

import java.time.LocalDate;
import java.util.List;

public record DayPlanDto(
		int dayNumber,
		LocalDate date,
		List<DayPlanItemDto> items
) {
}
