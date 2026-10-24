package com.vacationplan.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationplan.api.dto.TripRequest;
import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.BudgetLineItem;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.model.TransportMode;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.service.impl.BudgetCalculationServiceImpl;
import com.vacationplan.api.service.model.TripPlan;
import com.vacationplan.api.service.model.TripPlans;

class BudgetCalculatorTest {

	private final BudgetCalculationService calculator = new BudgetCalculationServiceImpl();

	@Test
	void calculatesRoundTripLodgingFoodAndIgnoresUnselectedActivities() {
		TripRequest request = new TripRequest(
				"Aksaray",
				"Isparta",
				LocalDate.of(2026, 7, 10),
				LocalDate.of(2026, 7, 12),
				2,
				List.of(),
				TransportMode.BUS,
				AccommodationType.HOTEL,
				TravelStyle.BALANCED,
				List.of("nature"),
				null,
				null,
				true,
				List.of("egirdir-golu"),
				null,
				List.of()
		);

		List<BudgetLineItem> items = List.of(
				new BudgetLineItem(
						BudgetCategory.TRANSPORT,
						"Gidiş-dönüş ulaşım",
						MoneyRange.ofTry(new BigDecimal("1600"), new BigDecimal("2200")),
						PriceSourceType.ESTIMATE,
						true
				),
				new BudgetLineItem(
						BudgetCategory.LODGING,
						"Konaklama",
						MoneyRange.ofTry(new BigDecimal("4800"), new BigDecimal("8800")),
						PriceSourceType.ESTIMATE,
						true
				),
				new BudgetLineItem(
						BudgetCategory.FOOD,
						"Yeme-içme",
						MoneyRange.ofTry(new BigDecimal("2100"), new BigDecimal("3900")),
						PriceSourceType.ESTIMATE,
						true
				),
				new BudgetLineItem(
						BudgetCategory.ACTIVITY,
						"Ücretsiz göl",
						MoneyRange.zeroTry(),
						PriceSourceType.ESTIMATE,
						true
				),
				new BudgetLineItem(
						BudgetCategory.ACTIVITY,
						"Davraz",
						MoneyRange.ofTry(new BigDecimal("400"), new BigDecimal("800")),
						PriceSourceType.ESTIMATE,
						false
				)
		);

		TripPlan plan = TripPlans.from(request);
		var breakdown = calculator.calculate(plan, items);

		assertEquals(2, plan.nightCount());
		assertEquals(3, plan.dayCount());
		assertEquals(0, breakdown.total().min().compareTo(new BigDecimal("8500")));
		assertTrue(breakdown.lineItems().stream().anyMatch(i -> i.label().equals("Davraz") && !i.selected()));
		assertTrue(breakdown.lineItems().stream().noneMatch(i -> i.label().contains("Beklenmeyen")));
		assertEquals(0, breakdown.byCategory().get(BudgetCategory.ACTIVITY).min().compareTo(BigDecimal.ZERO.setScale(2)));
	}

	@Test
	void adultEquivalentCountsChildrenAtHalf() {
		TripRequest request = new TripRequest(
				"Mersin",
				"Mersin",
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 2),
				2,
				List.of(8, 10),
				TransportMode.CAR,
				AccommodationType.NONE,
				TravelStyle.ECONOMIC,
				List.of(),
				null,
				null,
				false,
				List.of(),
				null,
				null
		);

		assertEquals(new BigDecimal("3.00"), calculator.adultEquivalent(TripPlans.from(request)).setScale(2));
	}
}
