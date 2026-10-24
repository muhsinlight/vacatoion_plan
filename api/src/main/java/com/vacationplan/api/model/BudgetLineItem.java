package com.vacationplan.api.model;

public record BudgetLineItem(
		BudgetCategory category,
		String label,
		MoneyRange amount,
		PriceSourceType sourceType,
		boolean selected
) {
}
