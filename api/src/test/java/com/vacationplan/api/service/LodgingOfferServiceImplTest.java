package com.vacationplan.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.client.RestClient;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.dto.scraper.LodgingSearchRequest;
import com.vacationplan.api.dto.scraper.LodgingSearchResponse;
import com.vacationplan.api.dto.scraper.LodgingSearchResponse.ScraperLodgingOffer;
import com.vacationplan.api.entity.LodgingOffer;
import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.LodgingTier;
import com.vacationplan.api.model.QuotaBucket;
import com.vacationplan.api.model.ResolvedLodging;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.repository.LodgingOfferRepository;
import com.vacationplan.api.service.impl.LodgingOfferServiceImpl;
import com.vacationplan.api.util.ScraperKeys;

@ExtendWith(MockitoExtension.class)
class LodgingOfferServiceImplTest {

	@Mock
	private LodgingOfferRepository repository;

	@Mock
	private RequestQuotaService requestQuotaService;

	@Mock
	private RestClient scraperRestClient;

	@Mock
	private RestClient.RequestBodyUriSpec requestSpec;

	@Mock
	private RestClient.ResponseSpec responseSpec;

	private VacationProperties properties;
	private final FlagTransactions transactions = new FlagTransactions();
	private LodgingOfferServiceImpl service;

	@BeforeEach
	void setUp() {
		properties = new VacationProperties();
		properties.setLodgingQuoteMaxAge(java.time.Duration.ofHours(12));
		Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC);
		service = new LodgingOfferServiceImpl(
				repository, properties, requestQuotaService, scraperRestClient, clock, transactions);
	}

	@Test
	void returnsUnavailableForCamp() {
		ResolvedLodging result = service.resolve(
				"istanbul",
				LocalDate.of(2026, 10, 29),
				LocalDate.of(2026, 11, 1),
				2,
				AccommodationType.CAMP,
				TravelStyle.BALANCED
		);
		assertEquals(ResolvedLodging.Status.UNAVAILABLE, result.status());
		verify(repository, never()).findByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
				anyString(), any(), any(), any());
	}

	@Test
	void usesFreshCache() {
		LodgingOffer budget = offer(LodgingTier.BUDGET, "1000");
		LodgingOffer mid = offer(LodgingTier.MID, "2000");
		LodgingOffer premium = offer(LodgingTier.PREMIUM, "4000");
		when(repository.findByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
				"istanbul", AccommodationType.HOTEL,
				LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 1)))
				.thenReturn(List.of(budget, mid, premium));

		ResolvedLodging result = service.resolve(
				"İstanbul",
				LocalDate.of(2026, 10, 29),
				LocalDate.of(2026, 11, 1),
				2,
				AccommodationType.HOTEL,
				TravelStyle.ECONOMIC
		);

		assertEquals(ResolvedLodging.Status.FRESH, result.status());
		assertEquals(LodgingTier.BUDGET, result.selectedTier());
		assertEquals(0, new BigDecimal("1000.00").compareTo(result.selectedOffer().pricePerNight()));
		assertEquals(0, new BigDecimal("1000.00").compareTo(result.perNightRange().min()));
		assertEquals(0, new BigDecimal("4000.00").compareTo(result.perNightRange().max()));
	}

	@Test
	void missingCacheStaysUnavailable() {
		when(repository.findByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
				anyString(), any(), any(), any()))
				.thenReturn(List.of());

		ResolvedLodging result = service.resolve(
				"istanbul",
				LocalDate.of(2026, 10, 29),
				LocalDate.of(2026, 11, 1),
				2,
				AccommodationType.HOTEL,
				TravelStyle.BALANCED
		);

		assertEquals(ResolvedLodging.Status.UNAVAILABLE, result.status());
		assertNull(result.selectedOffer());
		verify(scraperRestClient, never()).post();
	}

	@Test
	void estimatedCityRangeIsRefetched() {
		properties.setScraperTriggerKey("k".repeat(ScraperKeys.MIN_LENGTH));
		when(requestQuotaService.tryAcquire(QuotaBucket.SCRAPER)).thenReturn(true);
		LodgingOffer cached = offer(LodgingTier.MID, "2240");
		cached.setName("Obilet şehir aralığı");
		cached.setEstimated(true);
		when(repository.findByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
				anyString(), any(), any(), any()))
				.thenReturn(List.of(cached));
		when(scraperRestClient.post()).thenReturn(requestSpec);
		when(requestSpec.uri("/lodging/search")).thenReturn(requestSpec);
		when(requestSpec.header(eq(ScraperKeys.HEADER), anyString())).thenReturn(requestSpec);
		when(requestSpec.body(any(LodgingSearchRequest.class))).thenReturn(requestSpec);
		when(requestSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.body(LodgingSearchResponse.class)).thenReturn(new LodgingSearchResponse(
				"obilet", null, List.of(), List.of("boş"), false));

		service.resolve(
				"adana",
				LocalDate.of(2026, 10, 21),
				LocalDate.of(2026, 10, 23),
				1,
				AccommodationType.HOTEL,
				TravelStyle.BALANCED
		);

		verify(requestSpec).uri("/lodging/search");
	}

	@Test
	void liveScrapeRunsOutsideTransaction() {
		properties.setScraperTriggerKey("k".repeat(ScraperKeys.MIN_LENGTH));
		when(requestQuotaService.tryAcquire(QuotaBucket.SCRAPER)).thenReturn(true);
		when(repository.findByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
				anyString(), any(), any(), any()))
				.thenReturn(List.of());
		when(scraperRestClient.post()).thenReturn(requestSpec);
		when(requestSpec.uri("/lodging/search")).thenReturn(requestSpec);
		when(requestSpec.header(eq(ScraperKeys.HEADER), anyString())).thenReturn(requestSpec);
		when(requestSpec.body(any(LodgingSearchRequest.class))).thenAnswer(invocation -> {
			assertThat(transactions.active).isFalse();
			return requestSpec;
		});
		when(requestSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.body(LodgingSearchResponse.class)).thenReturn(new LodgingSearchResponse(
				"obilet", null, List.of(), List.of("boş"), false));

		service.resolve(
				"istanbul",
				LocalDate.of(2026, 10, 29),
				LocalDate.of(2026, 11, 1),
				2,
				AccommodationType.HOTEL,
				TravelStyle.BALANCED
		);
	}

	@Test
	void missingCacheRequestsObiletAndStoresOffers() {
		properties.setScraperTriggerKey("k".repeat(ScraperKeys.MIN_LENGTH));
		when(requestQuotaService.tryAcquire(QuotaBucket.SCRAPER)).thenReturn(true);
		List<LodgingOffer> stored = new java.util.ArrayList<>();
		when(repository.findByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
				anyString(), any(), any(), any()))
				.thenAnswer(invocation -> List.copyOf(stored));
		when(scraperRestClient.post()).thenReturn(requestSpec);
		when(requestSpec.uri("/lodging/search")).thenReturn(requestSpec);
		when(requestSpec.header(eq(ScraperKeys.HEADER), anyString())).thenReturn(requestSpec);
		when(requestSpec.body(any(LodgingSearchRequest.class))).thenReturn(requestSpec);
		when(requestSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.body(LodgingSearchResponse.class)).thenReturn(new LodgingSearchResponse(
				"obilet",
				new LodgingSearchResponse.LodgingMoneyBand(
						new BigDecimal("1500"), new BigDecimal("1500"), new BigDecimal("1500"), "TRY", 1),
				List.of(new ScraperLodgingOffer(
						LodgingTier.MID, "obilet", "Sahil Otel", 3, null, null, List.of(),
						new BigDecimal("1500"), new BigDecimal("4500"), null, null)),
				List.of(),
				false
		));
		when(repository.saveAll(any())).thenAnswer(invocation -> {
			stored.clear();
			stored.addAll(invocation.getArgument(0));
			return List.copyOf(stored);
		});

		ResolvedLodging result = service.resolve(
				"istanbul",
				LocalDate.of(2026, 10, 29),
				LocalDate.of(2026, 11, 1),
				2,
				AccommodationType.HOTEL,
				TravelStyle.BALANCED
		);

		assertEquals(ResolvedLodging.Status.FRESH, result.status());
		assertEquals(0, new BigDecimal("1500.00").compareTo(result.selectedOffer().pricePerNight()));
		verify(requestSpec).uri("/lodging/search");
		verify(repository).deleteByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
				"istanbul", AccommodationType.HOTEL,
				LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 1));
	}

	private static final class FlagTransactions implements TransactionOperations {
		boolean active;

		@Override
		public <T> T execute(TransactionCallback<T> action) {
			active = true;
			try {
				return action.doInTransaction(null);
			} finally {
				active = false;
			}
		}
	}

	private LodgingOffer offer(LodgingTier tier, String price) {
		LodgingOffer entity = new LodgingOffer();
		entity.setCityKey("istanbul");
		entity.setProvider("obilet");
		entity.setAccommodationType(AccommodationType.HOTEL);
		entity.setCheckIn(LocalDate.of(2026, 10, 29));
		entity.setCheckOut(LocalDate.of(2026, 11, 1));
		entity.setTier(tier);
		entity.setName(tier.name() + " Hotel");
		entity.setPricePerNight(new BigDecimal(price));
		entity.setBandMin(new BigDecimal("1000"));
		entity.setBandMax(new BigDecimal("4000"));
		entity.setCheckedAt(Instant.parse("2026-10-15T08:00:00Z"));
		return entity;
	}
}
