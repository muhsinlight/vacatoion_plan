package com.vacationplan.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.dto.scraper.PriceObservationBatchRequest;
import com.vacationplan.api.dto.scraper.PriceObservationDto;
import com.vacationplan.api.entity.Destination;
import com.vacationplan.api.entity.FuelPrice;
import com.vacationplan.api.entity.PriceObservation;
import com.vacationplan.api.entity.RouteQuote;
import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.FuelType;
import com.vacationplan.api.model.PriceScopeType;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.model.PriceUnit;
import com.vacationplan.api.model.TransportMode;
import com.vacationplan.api.repository.DestinationRepository;
import com.vacationplan.api.repository.FuelPriceRepository;
import com.vacationplan.api.repository.PriceObservationRepository;
import com.vacationplan.api.repository.RouteQuoteRepository;
import com.vacationplan.api.service.impl.PriceObservationIngestServiceImpl;
import com.vacationplan.api.service.impl.RouteQuoteMaterializer;

@ExtendWith(MockitoExtension.class)
class PriceObservationIngestServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-01T12:00:00Z"), ZoneOffset.UTC);
	private static final LocalDate OBSERVATION_DAY = LocalDate.of(2026, 7, 1);

	@Mock
	private PriceObservationRepository priceObservationRepository;

	@Mock
	private FuelPriceRepository fuelPriceRepository;

	@Mock
	private DestinationRepository destinationRepository;

	@Mock
	private RouteQuoteRepository routeQuoteRepository;

	private PriceObservationIngestService priceObservationIngestService;

	@BeforeEach
	void setUp() {
		var materializer = new RouteQuoteMaterializer(routeQuoteRepository);
		var properties = new VacationProperties();
		priceObservationIngestService = new PriceObservationIngestServiceImpl(
				priceObservationRepository,
				fuelPriceRepository,
				destinationRepository,
				materializer,
				properties,
				FIXED_CLOCK);
	}

	private void stubObservationPassthroughSave() {
		when(priceObservationRepository.save(any(PriceObservation.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void ingestRouteObservationMaterializesQuote() {
		stubObservationPassthroughSave();
		when(routeQuoteRepository.findByOriginKeyAndDestKeyAndTransportModeAndWeekBucket(
						"aksaray", "isparta", TransportMode.BUS, LocalDate.of(2026, 7, 6)))
				.thenReturn(Optional.empty());
		when(routeQuoteRepository.save(any(RouteQuote.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var dto = new PriceObservationDto(
				BudgetCategory.TRANSPORT,
				PriceScopeType.ROUTE,
				"aksaray|isparta|bus",
				PriceUnit.PER_PERSON,
				new BigDecimal("600"),
				new BigDecimal("750"),
				new BigDecimal("950"),
				"TRY",
				PriceSourceType.SCRAPE,
				"obilet",
				"https://obilet.com",
				8,
				new BigDecimal("0.85"),
				FIXED_CLOCK.instant(),
				"aksaray",
				"isparta",
				TransportMode.BUS,
				LocalDate.of(2026, 7, 6)
		);

		var response = priceObservationIngestService.ingestBatch(new PriceObservationBatchRequest(List.of(dto)));

		assertThat(response.saved()).isEqualTo(1);
		assertThat(response.routeQuotesUpdated()).isEqualTo(1);

		var captor = ArgumentCaptor.forClass(RouteQuote.class);
		verify(routeQuoteRepository).save(captor.capture());
		var quote = captor.getValue();
		assertThat(quote.getMinAmount()).isEqualByComparingTo("600");
		assertThat(quote.getTypicalAmount()).isEqualByComparingTo("750");
		assertThat(quote.getMaxAmount()).isEqualByComparingTo("950");
	}

	@Test
	void ingestFoodObservationUpdatesDestinationBand() {
		stubObservationPassthroughSave();
		var destination = new Destination();
		destination.setCityKey("isparta");
		when(destinationRepository.findByCityKey("isparta")).thenReturn(Optional.of(destination));

		var dto = new PriceObservationDto(
				BudgetCategory.FOOD,
				PriceScopeType.DESTINATION,
				"isparta",
				PriceUnit.PER_DAY,
				new BigDecimal("500.00"),
				new BigDecimal("900.00"),
				new BigDecimal("1600.00"),
				"TRY",
				PriceSourceType.OFFICIAL_API,
				"google-places",
				"https://places.googleapis.com",
				2,
				new BigDecimal("0.70"),
				FIXED_CLOCK.instant(),
				null,
				null,
				null,
				null
		);

		var response = priceObservationIngestService.ingestBatch(new PriceObservationBatchRequest(List.of(dto)));

		assertThat(response.saved()).isEqualTo(1);
		assertThat(destination.getFoodPerPersonPerDayMin()).isEqualByComparingTo("500.00");
		assertThat(destination.getFoodPerPersonPerDayMax()).isEqualByComparingTo("1600.00");
		assertThat(destination.getFoodSourceType()).isEqualTo(PriceSourceType.OFFICIAL_API);
		assertThat(destination.getFoodCheckedAt()).isEqualTo(FIXED_CLOCK.instant());
	}

	@Test
	void duplicateFoodObservationUpdatesSameRow() {
		var stored = new AtomicReference<PriceObservation>();
		var rows = new ArrayList<PriceObservation>();

		when(priceObservationRepository.findByScopeTypeAndScopeKeyAndUnitAndObservationDayAndWeekBucketIsNull(
						eq(PriceScopeType.DESTINATION), eq("isparta"), eq(PriceUnit.PER_DAY), eq(OBSERVATION_DAY)))
				.thenAnswer(invocation -> Optional.ofNullable(stored.get()));
		when(priceObservationRepository.save(any(PriceObservation.class))).thenAnswer(invocation -> {
			PriceObservation entity = invocation.getArgument(0);
			if (stored.get() == null) {
				stored.set(entity);
				rows.add(entity);
			}
			return entity;
		});
		when(priceObservationRepository.count()).thenAnswer(invocation -> (long) rows.size());

		var first = foodObservation("600.00", "1200.00");
		var second = foodObservation("650.00", "1300.00");

		priceObservationIngestService.ingestBatch(new PriceObservationBatchRequest(List.of(first)));
		priceObservationIngestService.ingestBatch(new PriceObservationBatchRequest(List.of(second)));

		assertThat(priceObservationRepository.count()).isEqualTo(1);
		assertThat(stored.get().getMaxAmount()).isEqualByComparingTo("1300.00");
	}

	@Test
	void duplicateFuelObservationUpsertsSingleFuelPriceRow() {
		stubObservationPassthroughSave();
		var stored = new AtomicReference<FuelPrice>();
		var rows = new ArrayList<FuelPrice>();

		when(fuelPriceRepository.findByFuelTypeAndEffectiveFrom(FuelType.GASOLINE, OBSERVATION_DAY))
				.thenAnswer(invocation -> Optional.ofNullable(stored.get()));
		when(fuelPriceRepository.save(any(FuelPrice.class))).thenAnswer(invocation -> {
			FuelPrice fuel = invocation.getArgument(0);
			stored.set(fuel);
			rows.clear();
			rows.add(fuel);
			return fuel;
		});
		when(fuelPriceRepository.count()).thenAnswer(invocation -> (long) rows.size());

		var dto = new PriceObservationDto(
				BudgetCategory.OTHER,
				PriceScopeType.NATIONAL_FUEL,
				"GASOLINE",
				PriceUnit.PER_LITER,
				new BigDecimal("40.00"),
				new BigDecimal("42.50"),
				new BigDecimal("45.00"),
				"TRY",
				PriceSourceType.SCRAPE,
				"fuel",
				"https://example.com/fuel",
				1,
				null,
				FIXED_CLOCK.instant(),
				null,
				null,
				null,
				null
		);
		var updated = new PriceObservationDto(
				BudgetCategory.OTHER,
				PriceScopeType.NATIONAL_FUEL,
				"GASOLINE",
				PriceUnit.PER_LITER,
				new BigDecimal("41.00"),
				new BigDecimal("43.00"),
				new BigDecimal("46.00"),
				"TRY",
				PriceSourceType.SCRAPE,
				"fuel",
				"https://example.com/fuel",
				1,
				null,
				FIXED_CLOCK.instant(),
				null,
				null,
				null,
				null
		);

		priceObservationIngestService.ingestBatch(new PriceObservationBatchRequest(List.of(dto)));
		priceObservationIngestService.ingestBatch(new PriceObservationBatchRequest(List.of(updated)));

		assertThat(fuelPriceRepository.findByFuelTypeAndEffectiveFrom(FuelType.GASOLINE, OBSERVATION_DAY)
						.orElseThrow()
						.getPricePerLiter())
				.isEqualByComparingTo("43.00");
		assertThat(fuelPriceRepository.count()).isEqualTo(1);
	}

	private static PriceObservationDto foodObservation(String min, String max) {
		return new PriceObservationDto(
				BudgetCategory.FOOD,
				PriceScopeType.DESTINATION,
				"isparta",
				PriceUnit.PER_DAY,
				new BigDecimal(min),
				new BigDecimal("900.00"),
				new BigDecimal(max),
				"TRY",
				PriceSourceType.OFFICIAL_API,
				"google-places",
				"https://places.googleapis.com",
				2,
				new BigDecimal("0.70"),
				FIXED_CLOCK.instant(),
				null,
				null,
				null,
				null
		);
	}
}
