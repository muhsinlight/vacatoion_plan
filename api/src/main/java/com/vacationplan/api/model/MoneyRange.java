package com.vacationplan.api.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

public record MoneyRange(BigDecimal min, BigDecimal max, String currency) {

	public static final String TRY = "TRY";

	public MoneyRange {
		if (min == null || max == null) {
			throw new IllegalArgumentException("min and max are required");
		}
		if (min.compareTo(max) > 0) {
			throw new IllegalArgumentException("min cannot exceed max");
		}
		if (currency == null || currency.isBlank()) {
			currency = TRY;
		}
		Currency.getInstance(currency);
		min = min.setScale(2, RoundingMode.HALF_UP);
		max = max.setScale(2, RoundingMode.HALF_UP);
	}

	public static MoneyRange ofTry(BigDecimal amount) {
		return new MoneyRange(amount, amount, TRY);
	}

	public static MoneyRange ofTry(BigDecimal min, BigDecimal max) {
		return new MoneyRange(min, max, TRY);
	}

	public static MoneyRange zeroTry() {
		return ofTry(BigDecimal.ZERO);
	}

	public MoneyRange plus(MoneyRange other) {
		ensureSameCurrency(other);
		return new MoneyRange(min.add(other.min), max.add(other.max), currency);
	}

	public MoneyRange multiply(BigDecimal factor) {
		return new MoneyRange(min.multiply(factor), max.multiply(factor), currency);
	}

	public MoneyRange multiply(int factor) {
		return multiply(BigDecimal.valueOf(factor));
	}

	private void ensureSameCurrency(MoneyRange other) {
		if (!currency.equals(other.currency)) {
			throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
		}
	}
}
