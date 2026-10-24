package com.vacationplan.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.vacationplan.api.entity.RouteQuote;
import com.vacationplan.api.model.MoneyRange;

class BusFareMathTest {

	@Test
	void weekBucketIsPreviousOrSameMonday() {
		assertEquals(LocalDate.of(2026, 10, 26), BusFareMath.weekBucket(LocalDate.of(2026, 10, 26)));
		assertEquals(LocalDate.of(2026, 10, 26), BusFareMath.weekBucket(LocalDate.of(2026, 10, 29)));
		assertEquals(LocalDate.of(2026, 10, 26), BusFareMath.weekBucket(LocalDate.of(2026, 11, 1)));
	}

	@Test
	void roundTripAddsBothDirectionsThenPassengers() {
		MoneyRange total = BusFareMath.roundTrip(quote("850", "900"), quote("800", "950"), new BigDecimal("2"));
		assertEquals(new BigDecimal("3300.00"), total.min());
		assertEquals(new BigDecimal("3700.00"), total.max());
	}

	private static RouteQuote quote(String min, String max) {
		RouteQuote quote = new RouteQuote();
		quote.setMinAmount(new BigDecimal(min));
		quote.setMaxAmount(new BigDecimal(max));
		return quote;
	}
}
