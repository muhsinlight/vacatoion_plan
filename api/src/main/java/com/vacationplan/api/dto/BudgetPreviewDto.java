package com.vacationplan.api.dto;

import java.util.List;

import com.vacationplan.api.model.BudgetBreakdown;

public record BudgetPreviewDto(
		MoneyDto total,
		MoneyDto perPerson,
		MoneyDto dailyAverage,
		List<BudgetLineDto> lines
) {
	public static BudgetPreviewDto from(BudgetBreakdown breakdown) {
		List<BudgetLineDto> lines = breakdown.lineItems().stream()
				.map(BudgetLineDto::fromSelected)
				.filter(line -> line != null)
				.toList();
		return new BudgetPreviewDto(
				MoneyDto.from(breakdown.total()),
				MoneyDto.from(breakdown.perPerson()),
				MoneyDto.from(breakdown.dailyAverage()),
				lines
		);
	}
}
