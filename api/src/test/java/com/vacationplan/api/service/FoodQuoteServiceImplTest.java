package com.vacationplan.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.client.RestClient;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.entity.Destination;
import com.vacationplan.api.model.QuotaBucket;
import com.vacationplan.api.service.FoodQuoteService.Result;
import com.vacationplan.api.service.impl.FoodQuoteServiceImpl;
import com.vacationplan.api.repository.DestinationRepository;
import com.vacationplan.api.util.ScraperKeys;

@ExtendWith(MockitoExtension.class)
class FoodQuoteServiceImplTest {

	@Mock
	private DestinationRepository destinationRepository;

	@Mock
	private RequestQuotaService requestQuotaService;

	@Mock
	private RestClient scraperRestClient;

	@Mock
	private RestClient.RequestBodyUriSpec requestSpec;

	@Mock
	private RestClient.ResponseSpec responseSpec;

	private final FlagTransactions transactions = new FlagTransactions();
	private VacationProperties properties;
	private Clock clock;
	private FoodQuoteServiceImpl service;

	@BeforeEach
	void setUp() {
		properties = new VacationProperties();
		properties.setFoodQuoteMaxAge(java.time.Duration.ofDays(7));
		clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC);
		service = new FoodQuoteServiceImpl(
				destinationRepository, properties, requestQuotaService, scraperRestClient, clock, transactions, task -> task.run());
	}

	@Test
	void staleDestinationScrapesOutsideTransaction() {
		properties.setScraperTriggerKey("k".repeat(ScraperKeys.MIN_LENGTH));
		Destination destination = new Destination();
		destination.setCityKey("istanbul");
		destination.setDisplayName("İstanbul");
		destination.setFoodCheckedAt(null);
		setDestinationId(destination, 3L);
		when(destinationRepository.findByCityKey("istanbul")).thenReturn(Optional.of(destination));
		when(requestQuotaService.tryAcquire(QuotaBucket.SCRAPER)).thenReturn(true);
		when(scraperRestClient.post()).thenReturn(requestSpec);
		when(requestSpec.uri("/food/search")).thenReturn(requestSpec);
		when(requestSpec.header(eq(ScraperKeys.HEADER), anyString())).thenReturn(requestSpec);
		when(requestSpec.body(any(foodSearchBodyClass()))).thenReturn(requestSpec);
		when(requestSpec.retrieve()).thenAnswer(invocation -> {
			assertThat(transactions.active).isFalse();
			return responseSpec;
		});
		when(responseSpec.body(com.vacationplan.api.dto.scraper.FoodSearchResponse.class)).thenReturn(null);

		assertThat(service.refreshIfStale("istanbul")).isEqualTo(Result.FAILED);
		verify(scraperRestClient).post();
	}

	@Test
	void staleDestinationReturnsScheduledWhenScraperDoesNotFinish() {
		properties.setScraperTriggerKey("k".repeat(ScraperKeys.MIN_LENGTH));
		properties.setFoodRefreshWait(java.time.Duration.ofMillis(20));
		Destination destination = new Destination();
		destination.setCityKey("istanbul");
		destination.setDisplayName("İstanbul");
		when(destinationRepository.findByCityKey("istanbul")).thenReturn(Optional.of(destination));
		service = new FoodQuoteServiceImpl(
				destinationRepository, properties, requestQuotaService, scraperRestClient, clock, transactions, command -> {
				});

		assertThat(service.refreshIfStale("istanbul")).isEqualTo(Result.SCHEDULED);
		verify(scraperRestClient, never()).post();
	}

	@Test
	void freshDestinationSkipsScraper() {
		Destination destination = new Destination();
		destination.setCityKey("istanbul");
		destination.setFoodCheckedAt(Instant.parse("2026-10-14T10:00:00Z"));
		destination.setFoodSourceType(com.vacationplan.api.model.PriceSourceType.OFFICIAL_API);
		when(destinationRepository.findByCityKey("istanbul")).thenReturn(Optional.of(destination));

		assertThat(service.refreshIfStale("istanbul")).isEqualTo(Result.FRESH);
		verify(scraperRestClient, never()).post();
	}

	@Test
	void recentEstimateWithoutPlacesSkipsScraper() {
		Destination destination = new Destination();
		destination.setCityKey("istanbul");
		destination.setFoodCheckedAt(Instant.parse("2026-10-15T09:50:00Z"));
		destination.setFoodSourceType(com.vacationplan.api.model.PriceSourceType.ESTIMATE);
		when(destinationRepository.findByCityKey("istanbul")).thenReturn(Optional.of(destination));

		assertThat(service.refreshIfStale("istanbul")).isEqualTo(Result.FRESH);
		verify(scraperRestClient, never()).post();
	}

	@SuppressWarnings("unchecked")
	private static Class<Object> foodSearchBodyClass() {
		for (Class<?> nested : FoodQuoteServiceImpl.class.getDeclaredClasses()) {
			if ("FoodSearchBody".equals(nested.getSimpleName())) {
				return (Class<Object>) nested;
			}
		}
		throw new IllegalStateException("FoodSearchBody bulunamadı");
	}

	private static void setDestinationId(Destination destination, long id) {
		try {
			var field = Destination.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(destination, id);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
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
}
