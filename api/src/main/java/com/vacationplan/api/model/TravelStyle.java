package com.vacationplan.api.model;

import java.math.BigDecimal;

public enum TravelStyle {
	ECONOMIC(new BigDecimal("0.75")),
	BALANCED(BigDecimal.ONE),
	COMFORT(new BigDecimal("1.35"));

	private final BigDecimal foodMultiplier;

	TravelStyle(BigDecimal foodMultiplier) {
		this.foodMultiplier = foodMultiplier;
	}

	public BigDecimal foodMultiplier() {
		return foodMultiplier;
	}
}
