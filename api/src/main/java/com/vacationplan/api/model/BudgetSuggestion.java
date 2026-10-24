package com.vacationplan.api.model;

import java.math.BigDecimal;

public record BudgetSuggestion(
		String code,
		String message,
		BigDecimal savingMin,
		BigDecimal savingMax,
		String currency
) {
}
