package com.vacationplan.api.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.web.client.RestClient;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.dto.scraper.BusRouteBatchRequest;
import com.vacationplan.api.dto.scraper.BusRouteBatchResponse;
import com.vacationplan.api.entity.PriceObservation;
import com.vacationplan.api.entity.RouteQuote;
import com.vacationplan.api.model.QuotaBucket;
import com.vacationplan.api.model.ResolvedBusFare;
import com.vacationplan.api.repository.RouteQuoteRepository;
import com.vacationplan.api.service.impl.BusRouteFareServiceImpl;
import com.vacationplan.api.util.ScraperKeys;

@ExtendWith(MockitoExtension.class)
class BusRouteFareServiceImplTest {

	@Mock
	private RouteQuoteRepository repository;

	@Mock
	private RequestQuotaService requestQuotaService;

	@Mock
	private RestClient scraperRestClient;

	@Mock
	private RestClient.RequestBodyUriSpec requestSpec;

	@Mock
	private RestClient.ResponseSpec responseSpec;

	private VacationProperties properties;
	private Clock clock;
	private BusRouteFareServiceImpl service;

	@BeforeEach
	void setUp() {
		properties = new VacationProperties();
		clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC);
		service = new BusRouteFareServiceImpl(
				repository, properties, requestQuotaService, scraperRestClient, clock);
	}

	private static final LocalDate WEEK = LocalDate.of(2026, 10, 26);

	@Test
	void usesFreshQuotesWithoutCallingScraper() {
		when(repository.findBusRoundTrip(anyString(), anyString(), any(), any(), any()))
				.thenReturn(List.of(leg("aksaray", "isparta", "500", "700"), leg("isparta", "aksaray", "500", "700")));

		ResolvedBusFare fare = service.roundTrip(
				"Aksaray", "Isparta", LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 1), BigDecimal.ONE);

		assertThat(fare.status()).isEqualTo(ResolvedBusFare.Status.FRESH);
		assertThat(fare.amount().min()).isEqualByComparingTo("1000.00");
		verify(scraperRestClient, never()).post();
	}

	@Test
	void missingQuoteStaysUnavailable() {
		when(repository.findBusRoundTrip(anyString(), anyString(), any(), any(), any()))
				.thenReturn(List.of());

		ResolvedBusFare fare = service.roundTrip(
				"Aksaray", "Isparta", LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 1), BigDecimal.TWO);

		assertThat(fare.status()).isEqualTo(ResolvedBusFare.Status.UNAVAILABLE);
		assertThat(fare.amount()).isNull();
	}

	@Test
	void staleQuoteIsNotUsed() {
		RouteQuote stale = leg("aksaray", "isparta", "500", "700");
		stale.setCheckedAt(Instant.parse("2026-10-14T10:00:00Z"));
		when(repository.findBusRoundTrip(anyString(), anyString(), any(), any(), any()))
				.thenReturn(List.of(stale, leg("isparta", "aksaray", "500", "700")));

		ResolvedBusFare fare = service.roundTrip(
				"Aksaray", "Isparta", LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 1), BigDecimal.ONE);

		assertThat(fare.status()).isEqualTo(ResolvedBusFare.Status.UNAVAILABLE);
		assertThat(fare.legs()).isEmpty();
	}

	@Test
	void freshQuoteCarriesCheapestTicketProof() {
		RouteQuote outbound = leg("aksaray", "isparta", "500", "700");
		PriceObservation observation = new PriceObservation();
		observation.setProvider("Pamukkale Turizm");
		observation.setSourceUrl("https://www.obilet.com/seferler/414-381/2026-10-29");
		outbound.setObservation(observation);
		outbound.setCurrency("TRY");
		RouteQuote inbound = leg("isparta", "aksaray", "400", "600");
		inbound.setCurrency("TRY");
		when(repository.findBusRoundTrip(anyString(), anyString(), any(), any(), any()))
				.thenReturn(List.of(outbound, inbound));

		ResolvedBusFare fare = service.roundTrip(
				"Aksaray", "Isparta", LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 1), BigDecimal.ONE);

		assertThat(fare.legs()).extracting(leg -> leg.direction()).containsExactly("Gidiş", "Dönüş");
		assertThat(fare.legs().get(0).partner()).isEqualTo("Pamukkale Turizm");
		assertThat(fare.legs().get(0).url()).isEqualTo("https://www.obilet.com/seferler/414-381/2026-10-29");
		assertThat(fare.legs().get(0).minAmount()).isEqualByComparingTo("500");
		assertThat(fare.legs().get(1).partner()).isNull();
		assertThat(fare.legs().get(1).url()).isNull();
	}

	@Test
	void missingQuoteWithoutQuotaStaysUnavailable() {
		properties.setScraperTriggerKey("k".repeat(ScraperKeys.MIN_LENGTH));
		when(requestQuotaService.tryAcquire(QuotaBucket.SCRAPER)).thenReturn(false);
		when(repository.findBusRoundTrip(anyString(), anyString(), any(), any(), any())).thenReturn(List.of());

		ResolvedBusFare fare = service.roundTrip(
				"Aksaray", "Isparta", LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 1), BigDecimal.ONE);

		assertThat(fare.status()).isEqualTo(ResolvedBusFare.Status.UNAVAILABLE);
		verify(scraperRestClient, never()).post();
	}

	@Test
	void missingQuoteRequestsObiletThenReadsSavedFare() {
		properties.setScraperTriggerKey("k".repeat(ScraperKeys.MIN_LENGTH));
		when(requestQuotaService.tryAcquire(QuotaBucket.SCRAPER)).thenReturn(true);
		when(repository.findBusRoundTrip(anyString(), anyString(), any(), any(), any()))
				.thenReturn(List.of())
				.thenReturn(List.of(leg("aksaray", "isparta", "450", "600"), leg("isparta", "aksaray", "450", "600")));
		when(scraperRestClient.post()).thenReturn(requestSpec);
		when(requestSpec.uri("/routes/bus")).thenReturn(requestSpec);
		when(requestSpec.header(eq(ScraperKeys.HEADER), anyString())).thenReturn(requestSpec);
		when(requestSpec.body(any(BusRouteBatchRequest.class))).thenReturn(requestSpec);
		when(requestSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.body(BusRouteBatchResponse.class)).thenReturn(new BusRouteBatchResponse(2, List.of()));

		ResolvedBusFare fare = service.roundTrip(
				"Aksaray", "Isparta", LocalDate.of(2026, 10, 29), LocalDate.of(2026, 11, 1), BigDecimal.ONE);

		assertThat(fare.status()).isEqualTo(ResolvedBusFare.Status.FRESH);
		assertThat(fare.amount().min()).isEqualByComparingTo("900.00");
		verify(requestSpec).uri("/routes/bus");
	}

	private static RouteQuote leg(String origin, String dest, String min, String max) {
		RouteQuote quote = new RouteQuote();
		quote.setOriginKey(origin);
		quote.setDestKey(dest);
		quote.setWeekBucket(WEEK);
		quote.setMinAmount(new BigDecimal(min));
		quote.setMaxAmount(new BigDecimal(max));
		quote.setCheckedAt(Instant.parse("2026-10-15T08:00:00Z"));
		return quote;
	}
}
