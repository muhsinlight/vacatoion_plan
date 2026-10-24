package com.vacationplan.api.dto;

import java.math.BigDecimal;

public record BudgetSuggestionDto(
		String code,
		String message,
		BigDecimal savingMin,
		BigDecimal savingMax,
		String currency
) {
	public static BudgetSuggestionDto from(com.vacationplan.api.model.BudgetSuggestion suggestion) {
		return new BudgetSuggestionDto(
				suggestion.code(),
				suggestion.message(),
				suggestion.savingMin(),
				suggestion.savingMax(),
				suggestion.currency()
		);
	}
}
