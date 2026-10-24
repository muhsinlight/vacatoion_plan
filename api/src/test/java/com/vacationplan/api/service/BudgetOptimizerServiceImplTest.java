package com.vacationplan.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.BudgetBreakdown;
import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.BudgetLineItem;
import com.vacationplan.api.model.BudgetSuggestion;
import com.vacationplan.api.model.LodgingOffer;
import com.vacationplan.api.model.LodgingTier;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.model.ResolvedLodging;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.model.catalog.DestinationProfile;
import com.vacationplan.api.service.impl.BudgetOptimizerServiceImpl;
import com.vacationplan.api.service.model.TripPlan;
import com.vacationplan.api.service.model.TripStay;

@ExtendWith(MockitoExtension.class)
class BudgetOptimizerServiceImplTest {

	@Mock
	private CatalogService catalogService;

	@Test
	void contingencyBoundsMatchTheTenPercentLine() {
		BudgetOptimizerServiceImpl optimizer = new BudgetOptimizerServiceImpl(catalogService);
		BudgetLineItem contingency = new BudgetLineItem(
				BudgetCategory.OTHER,
				"Beklenmeyen gider payı (%10)",
				MoneyRange.ofTry(new BigDecimal("80"), new BigDecimal("120")),
				PriceSourceType.ESTIMATE,
				true
		);

		List<BudgetSuggestion> suggestions = optimizer.suggest(
				plan(TravelStyle.ECONOMIC, AccommodationType.NONE, 0),
				profile(),
				breakdown(List.of(contingency), "1000"),
				null
		);

		BudgetSuggestion found = suggestions.stream()
				.filter(suggestion -> suggestion.code().equals("REDUCE_CONTINGENCY"))
				.findFirst()
				.orElseThrow();
		assertEquals(0, found.savingMin().compareTo(new BigDecimal("80.00")));
		assertEquals(0, found.savingMax().compareTo(new BigDecimal("120.00")));
	}

	@Test
	void skipsContingencyWhenTheLineIsMissing() {
		BudgetOptimizerServiceImpl optimizer = new BudgetOptimizerServiceImpl(catalogService);

		List<BudgetSuggestion> suggestions = optimizer.suggest(
				plan(TravelStyle.ECONOMIC, AccommodationType.NONE, 0),
				profile(),
				breakdown(List.of(), "1000"),
				null
		);

		assertTrue(suggestions.stream().noneMatch(suggestion -> suggestion.code().equals("REDUCE_CONTINGENCY")));
	}

	@Test
	void campSavingUsesSelectedLodgingNotCatalogHotel() {
		when(catalogService.lodgingQuote(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(AccommodationType.CAMP)))
				.thenReturn(MoneyRange.ofTry(new BigDecimal("100"), new BigDecimal("200")));
		BudgetOptimizerServiceImpl optimizer = new BudgetOptimizerServiceImpl(catalogService);
		ResolvedLodging lodging = new ResolvedLodging(
				"live",
				MoneyRange.ofTry(new BigDecimal("4000")),
				List.of(new LodgingOffer(
						LodgingTier.MID, null, "Canlı otel", null, null, null, List.of(),
						new BigDecimal("4000"), null, null)),
				LodgingTier.MID,
				ResolvedLodging.Status.FRESH,
				false
		);

		BudgetSuggestion camp = optimizer.suggest(
				plan(TravelStyle.ECONOMIC, AccommodationType.HOTEL, 2),
				profile(),
				breakdown(List.of(), "1000"),
				lodging
		).stream().filter(suggestion -> suggestion.code().equals("LODGING_CAMP")).findFirst().orElseThrow();

		assertEquals(0, camp.savingMin().compareTo(new BigDecimal("7600.00")));
		assertEquals(0, camp.savingMax().compareTo(new BigDecimal("7800.00")));
	}

	private static TripPlan plan(TravelStyle style, AccommodationType lodging, int nights) {
		LocalDate start = LocalDate.of(2026, 7, 10);
		return new TripPlan(
				"Aksaray",
				"Isparta",
				new TripStay(start, start.plusDays(nights)),
				2,
				List.of(),
				2,
				null,
				lodging,
				style,
				new BigDecimal("10"),
				null,
				true,
				List.of(),
				List.of()
		);
	}

	private static DestinationProfile profile() {
		return new DestinationProfile(
				"isparta",
				"Isparta",
				MoneyRange.ofTry(new BigDecimal("900"), new BigDecimal("2200")),
				MoneyRange.ofTry(new BigDecimal("810"), new BigDecimal("1980")),
				MoneyRange.ofTry(new BigDecimal("2160"), new BigDecimal("5280")),
				MoneyRange.ofTry(new BigDecimal("1035"), new BigDecimal("2530")),
				MoneyRange.ofTry(new BigDecimal("1500"), new BigDecimal("4500")),
				MoneyRange.ofTry(new BigDecimal("600"), new BigDecimal("1400")),
				PriceSourceType.ESTIMATE,
				MoneyRange.ofTry(new BigDecimal("150"), new BigDecimal("400")),
				List.of(),
				List.of()
		);
	}

	private static BudgetBreakdown breakdown(List<BudgetLineItem> lines, String totalMin) {
		Map<BudgetCategory, MoneyRange> byCategory = new EnumMap<>(BudgetCategory.class);
		return new BudgetBreakdown(
				lines,
				byCategory,
				MoneyRange.ofTry(new BigDecimal(totalMin), new BigDecimal(totalMin)),
				MoneyRange.zeroTry(),
				MoneyRange.zeroTry(),
				MoneyRange.zeroTry(),
				MoneyRange.zeroTry()
		);
	}
}
