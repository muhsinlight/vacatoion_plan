package com.vacationplan.api.dto;

import java.math.BigDecimal;

import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.BudgetLineItem;
import com.vacationplan.api.model.PriceSourceType;

public record BudgetLineDto(
		BudgetCategory category,
		String label,
		BigDecimal min,
		BigDecimal max,
		String currency,
		PriceSourceType sourceType
) {
	public static BudgetLineDto fromSelected(BudgetLineItem item) {
		if (!item.selected()) {
			return null;
		}
		return new BudgetLineDto(
				item.category(),
				item.label(),
				item.amount().min(),
				item.amount().max(),
				item.amount().currency(),
				item.sourceType()
		);
	}
}
