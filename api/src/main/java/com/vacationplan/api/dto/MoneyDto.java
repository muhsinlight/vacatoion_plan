package com.vacationplan.api.dto;

import java.math.BigDecimal;

import com.vacationplan.api.model.MoneyRange;

public record MoneyDto(BigDecimal min, BigDecimal max, String currency) {

	public static MoneyDto from(MoneyRange range) {
		return new MoneyDto(range.min(), range.max(), range.currency());
	}
}
