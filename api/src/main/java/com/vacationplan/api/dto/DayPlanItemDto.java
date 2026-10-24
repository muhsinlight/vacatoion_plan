package com.vacationplan.api.dto;

public record DayPlanItemDto(
		String type,
		String title,
		int estimatedMinutes,
		String note
) {
}
