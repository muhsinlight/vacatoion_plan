package com.vacationplan.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vacationplan.api.dto.TripRequest;
import com.vacationplan.api.model.ActivityOption;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.service.impl.ItineraryServiceImpl;
import com.vacationplan.api.service.model.TripPlans;

class ItineraryServiceImplTest {

	private final ItineraryService itineraryService = new ItineraryServiceImpl();

	@Test
	void distributesActivitiesAcrossDays() {
		var request = new TripRequest(
				"Aksaray",
				"Isparta",
				LocalDate.of(2026, 7, 10),
				LocalDate.of(2026, 7, 12),
				2,
				List.of(),
				null,
				null,
				null,
				List.of(),
				null,
				null,
				true,
				List.of("a1", "a2"),
				null,
				List.of("Tekne turu")
		);
		var activities = List.of(
				activity("a1", "Göl"),
				activity("a2", "Müze")
		);

		var plan = itineraryService.build(TripPlans.from(request), activities, List.of("a1", "a2"));

		assertThat(plan).hasSize(3);
		assertThat(plan.get(0).items()).anyMatch(i -> i.title().equals("Göl"));
		assertThat(plan.get(1).items()).anyMatch(i -> i.title().equals("Müze"));
		assertThat(plan.get(2).items()).anyMatch(i -> i.title().equals("Tekne turu"));
	}

	private static ActivityOption activity(String id, String name) {
		return new ActivityOption(
				id,
				name,
				null,
				120,
				MoneyRange.ofTry(BigDecimal.ZERO),
				PriceSourceType.ESTIMATE,
				null,
				false
		);
	}
}
