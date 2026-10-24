package com.vacationplan.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import com.vacationplan.api.entity.Trip;
import com.vacationplan.api.entity.TripBudgetLine;
import com.vacationplan.api.entity.User;
import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.model.TripStatus;
import com.vacationplan.api.repository.DestinationRepository;
import com.vacationplan.api.repository.TripBudgetLineRepository;
import com.vacationplan.api.repository.TripRepository;
import com.vacationplan.api.security.CurrentUser;
import com.vacationplan.api.service.impl.TripServiceImpl;

@ExtendWith(MockitoExtension.class)
class TripListBudgetTest {

	@Mock
	private TripRepository tripRepository;
	@Mock
	private DestinationRepository destinationRepository;
	@Mock
	private TripBudgetLineRepository tripBudgetLineRepository;
	@Mock
	private CurrentUser currentUser;
	@Mock
	private TripPreviewService tripPreviewService;
	@Mock
	private PriceSnapshotService priceSnapshotService;
	@Mock
	private TransactionOperations transactions;

	private TripServiceImpl service;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-07-01T12:00:00Z"), ZoneOffset.UTC);
		service = new TripServiceImpl(
				tripRepository,
				destinationRepository,
				tripBudgetLineRepository,
				tripPreviewService,
				priceSnapshotService,
				currentUser,
				clock,
				transactions);
	}

	@Test
	void listMineIncludesStoredBudgetTotals() {
		User owner = new User();
		owner.setId(3L);
		when(currentUser.requireEntity()).thenReturn(owner);

		Trip trip = mock(Trip.class);
		when(trip.getId()).thenReturn(9L);
		when(trip.getStatus()).thenReturn(TripStatus.DRAFT);
		when(trip.getOriginCity()).thenReturn("istanbul");
		when(trip.getDestinationCity()).thenReturn("antalya");
		when(trip.getStartDate()).thenReturn(LocalDate.of(2026, 8, 1));
		when(trip.getEndDate()).thenReturn(LocalDate.of(2026, 8, 5));
		when(trip.getAdults()).thenReturn(2);
		when(trip.getChildAges()).thenReturn(List.of());
		var pageable = PageRequest.of(0, 20, Sort.by("id").ascending());
		when(tripRepository.findByOwnerIdOrderByIdAsc(3L, pageable))
				.thenReturn(new PageImpl<>(List.of(trip), pageable, 1));

		TripBudgetLine line = new TripBudgetLine();
		line.setCategory(BudgetCategory.TRANSPORT);
		line.setLabel("Otobüs");
		line.setMinAmount(new BigDecimal("100"));
		line.setMaxAmount(new BigDecimal("200"));
		line.setCurrency("TRY");
		line.setSourceType(PriceSourceType.SCRAPE);
		line.setIncludedInTotal(true);
		line.setTrip(trip);
		when(tripBudgetLineRepository.findByTrip_IdInOrderByIdAsc(List.of(9L))).thenReturn(List.of(line));

		var page = service.listMine(0, 20);

		assertThat(page.content()).hasSize(1);
		assertThat(page.content().get(0).budget()).isNotNull();
		assertThat(page.content().get(0).budget().total().min()).isEqualByComparingTo("100");
		assertThat(page.totalElements()).isEqualTo(1);
	}
}
