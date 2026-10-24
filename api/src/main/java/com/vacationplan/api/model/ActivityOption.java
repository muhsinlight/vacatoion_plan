package com.vacationplan.api.model;

public record ActivityOption(
		String id,
		String name,
		String description,
		int estimatedMinutes,
		MoneyRange entryFeePerPerson,
		PriceSourceType sourceType,
		String sourceUrl,
		boolean defaultSelected
) {
}
