package com.vacationplan.api.model;

import java.time.Instant;

public record PriceQuote(
		BudgetCategory category,
		String label,
		MoneyRange amount,
		PriceSourceType sourceType,
		String sourceUrl,
		Instant checkedAt,
		String notes
) {
}
