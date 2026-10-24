package com.vacationplan.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import com.vacationplan.api.entity.User;
import com.vacationplan.api.entity.Trip;
import com.vacationplan.api.exception.TripNotFoundException;
import com.vacationplan.api.model.TripStatus;
import com.vacationplan.api.repository.DestinationRepository;
import com.vacationplan.api.repository.TripBudgetLineRepository;
import com.vacationplan.api.repository.TripRepository;
import com.vacationplan.api.security.CurrentUser;
import com.vacationplan.api.service.impl.TripServiceImpl;

@ExtendWith(MockitoExtension.class)
class TripOwnershipTest {

	@Mock
	private TripRepository tripRepository;
	@Mock
	private DestinationRepository destinationRepository;
	@Mock
	private TripBudgetLineRepository tripBudgetLineRepository;
	@Mock
	private TripPreviewService tripPreviewService;
	@Mock
	private PriceSnapshotService priceSnapshotService;
	@Mock
	private CurrentUser currentUser;

	private final FlagTransactions transactions = new FlagTransactions();
	private TripService tripService;

	@BeforeEach
	void setUp() {
		tripService = new TripServiceImpl(
				tripRepository,
				destinationRepository,
				tripBudgetLineRepository,
				tripPreviewService,
				priceSnapshotService,
				currentUser,
				Clock.systemUTC(),
				transactions
		);
	}

	@Test
	void refreshBudgetPlansOutsideTransaction() {
		User ada = user(1L);
		when(currentUser.requireEntity()).thenReturn(ada);
		when(tripRepository.findById(8L)).thenReturn(Optional.of(trip(8L, ada)));
		when(tripPreviewService.plan(any())).thenAnswer(invocation -> {
			assertThat(transactions.active).isFalse();
			throw new IllegalStateException("plan çağrıldı");
		});

		assertThatThrownBy(() -> tripService.refreshBudget(8L)).hasMessage("plan çağrıldı");
		assertThat(transactions.count).isEqualTo(1);
	}

	@Test
	void foreignTripIsRejectedBeforeScraping() {
		when(currentUser.requireEntity()).thenReturn(user(1L));
		when(tripRepository.findById(9L)).thenReturn(Optional.of(trip(9L, user(2L))));

		assertThatThrownBy(() -> tripService.refreshBudget(9L)).isInstanceOf(TripNotFoundException.class);
		verify(tripPreviewService, never()).plan(any());
	}

	private static final class FlagTransactions implements TransactionOperations {
		boolean active;
		int count;

		@Override
		public <T> T execute(TransactionCallback<T> action) {
			active = true;
			count++;
			try {
				return action.doInTransaction(null);
			} finally {
				active = false;
			}
		}
	}

	@Test
	void ownerReadsOwnTripAndOtherIdsStayMissing() {
		User ada = user(1L);
		User grace = user(2L);
		when(currentUser.requireEntity()).thenReturn(ada);
		when(tripRepository.findById(8L)).thenReturn(Optional.of(trip(8L, ada)));
		when(tripBudgetLineRepository.findByTripIdOrderByIdAsc(8L)).thenReturn(List.of());

		assertThat(tripService.getById(8L).id()).isEqualTo(8L);

		when(tripRepository.findById(9L)).thenReturn(Optional.of(trip(9L, grace)));
		assertThatThrownBy(() -> tripService.getById(9L)).isInstanceOf(TripNotFoundException.class);

		when(tripRepository.findById(10L)).thenReturn(Optional.of(trip(10L, null)));
		assertThatThrownBy(() -> tripService.getById(10L)).isInstanceOf(TripNotFoundException.class);
	}

	private static User user(Long id) {
		User user = new User();
		user.setId(id);
		user.setEmail("user" + id + "@example.com");
		user.setPasswordHash("hash");
		return user;
	}

	private static Trip trip(Long id, User owner) {
		Trip trip = new Trip();
		trip.setOriginCity("Aksaray");
		trip.setDestinationCity("Isparta");
		trip.setStartDate(LocalDate.of(2026, 7, 10));
		trip.setEndDate(LocalDate.of(2026, 7, 12));
		trip.setAdults(2);
		trip.setStatus(TripStatus.PLANNED);
		trip.setOwner(owner);
		try {
			var field = Trip.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(trip, id);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
		return trip;
	}
}
