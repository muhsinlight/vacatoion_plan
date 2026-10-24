package com.vacationplan.api.model;

import java.util.List;
import java.util.Map;

public record BudgetBreakdown(
		List<BudgetLineItem> lineItems,
		Map<BudgetCategory, MoneyRange> byCategory,
		MoneyRange total,
		MoneyRange perPerson,
		MoneyRange dailyAverage,
		MoneyRange estimatedOnlyTotal,
		MoneyRange knownPriceTotal
) {
}
