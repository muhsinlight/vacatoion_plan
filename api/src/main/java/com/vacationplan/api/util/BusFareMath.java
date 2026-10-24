package com.vacationplan.api.util;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.vacationplan.api.entity.RouteQuote;
import com.vacationplan.api.model.MoneyRange;

public final class BusFareMath {

	private BusFareMath() {
	}

	/** Python week_bucket ile aynı: haftanın pazartesisi. */
	public static LocalDate weekBucket(LocalDate day) {
		return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	public static MoneyRange oneWay(RouteQuote quote) {
		return MoneyRange.ofTry(quote.getMinAmount(), quote.getMaxAmount());
	}

	public static MoneyRange roundTrip(RouteQuote outbound, RouteQuote inbound, BigDecimal passengers) {
		return oneWay(outbound).plus(oneWay(inbound)).multiply(passengers);
	}
}
