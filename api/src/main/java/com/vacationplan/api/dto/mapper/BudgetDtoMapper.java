package com.vacationplan.api.dto.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.vacationplan.api.dto.BudgetLineDto;
import com.vacationplan.api.dto.BudgetPreviewDto;
import com.vacationplan.api.dto.MoneyDto;
import com.vacationplan.api.entity.Trip;
import com.vacationplan.api.entity.TripBudgetLine;
import com.vacationplan.api.model.BudgetBreakdown;
import com.vacationplan.api.model.MoneyRange;

public final class BudgetDtoMapper {

	private BudgetDtoMapper() {
	}

	public static BudgetPreviewDto fromBreakdown(BudgetBreakdown breakdown) {
		return BudgetPreviewDto.from(breakdown);
	}

	public static BudgetPreviewDto fromStoredLines(Trip trip, List<TripBudgetLine> lines) {
		if (lines.isEmpty()) {
			return null;
		}
		MoneyRange total = lines.stream()
				.filter(line -> line.isIncludedInTotal())
				.map(line -> MoneyRange.ofTry(line.getMinAmount(), line.getMaxAmount()))
				.reduce(MoneyRange.zeroTry(), (left, right) -> left.plus(right));

		List<BudgetLineDto> lineDtos = lines.stream()
				.filter(line -> line.isIncludedInTotal())
				.map(line -> new BudgetLineDto(
						line.getCategory(),
						line.getLabel(),
						line.getMinAmount(),
						line.getMaxAmount(),
						line.getCurrency(),
						null
				))
				.toList();

		int travelers = Math.max(trip.getAdults() + trip.getChildAges().size(), 1);
		long daySpan = java.time.temporal.ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate());
		int days = Math.max((int) daySpan, 0) + 1;

		MoneyDto perPerson = MoneyDto.from(total.multiply(
				BigDecimal.ONE.divide(BigDecimal.valueOf(travelers), 8, RoundingMode.HALF_UP)));
		MoneyDto daily = MoneyDto.from(total.multiply(
				BigDecimal.ONE.divide(BigDecimal.valueOf(days), 8, RoundingMode.HALF_UP)));

		return new BudgetPreviewDto(MoneyDto.from(total), perPerson, daily, lineDtos);
	}
}
