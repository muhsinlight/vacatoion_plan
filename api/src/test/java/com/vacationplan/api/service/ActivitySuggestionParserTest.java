package com.vacationplan.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.dto.ActivitySuggestionRequest;
import com.vacationplan.api.entity.Destination;
import com.vacationplan.api.exception.ActivitySuggestionException;
import com.vacationplan.api.exception.QuotaExceededException;
import com.vacationplan.api.model.QuotaBucket;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.repository.CatalogActivityRepository;
import com.vacationplan.api.repository.DestinationRepository;
import com.vacationplan.api.service.impl.ActivitySuggestionServiceImpl;

class ActivitySuggestionParserTest {

	@Test
	void parsesJsonArrayAndDropsBlankNames() {
		var activities = ActivitySuggestionParser.parse("""
				```json
				[{"name":"Narlıkuyu","description":"Mozaikli koy"},{"name":"  ","description":"boş"}]
				```
				""");

		assertEquals(1, activities.size());
		assertEquals("Narlıkuyu", activities.get(0).name());
	}

	@Test
	void rejectsPriceField() {
		ActivitySuggestionException ex = assertThrows(ActivitySuggestionException.class, () -> ActivitySuggestionParser.parse("""
				[{"name":"Kale","price":100}]
				"""));
		assertEquals("Öneri yanıtında izin verilmeyen alan var.", ex.getMessage());
		assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
	}

	@Test
	void rejectsTextThatIsNotJson() {
		assertThrows(ActivitySuggestionException.class, () -> ActivitySuggestionParser.parse("Mersin çok güzel."));
	}

	@Test
	void promptDoesNotAskForPrice() throws Exception {
		String prompt = new ClassPathResource("prompts/activity-suggest.txt").getContentAsString(StandardCharsets.UTF_8);
		assertTrue(prompt.contains("fiyat yazma"));
	}

	@Test
	void readsGroqMessageContentAsText() {
		VacationProperties properties = new VacationProperties();
		properties.getGroq().setApiKey("test-key");
		String payload = """
				{"choices":[{"message":{"content":"[{\\"name\\":\\"Kale\\",\\"description\\":\\"Sur\\"}]"}}]}
				""";
		RestClient client = RestClient.builder()
				.baseUrl("http://groq.test")
				.requestInterceptor((request, body, execution) -> {
					MockClientHttpResponse response = new MockClientHttpResponse(
							payload.getBytes(StandardCharsets.UTF_8),
							HttpStatus.OK
					);
					response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
					return response;
				})
				.build();
		DestinationRepository destinations = mock(DestinationRepository.class);
		Destination city = new Destination();
		city.setDisplayName("Ankara");
		when(destinations.findByCityKey(anyString())).thenReturn(Optional.of(city));
		CatalogActivityRepository catalog = mock(CatalogActivityRepository.class);
		when(catalog.findByDestination_CityKey(anyString())).thenReturn(List.of());
		RequestQuotaService quota = mock(RequestQuotaService.class);
		when(quota.tryAcquire(QuotaBucket.GROQ)).thenReturn(true);
		var service = new ActivitySuggestionServiceImpl(properties, destinations, catalog, client, quota);

		var activities = service.suggest("ankara", new ActivitySuggestionRequest(null, null, 2, TravelStyle.COMFORT));

		assertEquals(1, activities.size());
		assertEquals("Kale", activities.get(0).name());
	}

	@Test
	void missingKeyDoesNotCallGroq() {
		VacationProperties properties = new VacationProperties();
		properties.getGroq().setApiKey("");
		RestClient client = RestClient.builder()
				.requestInterceptor((request, body, execution) -> {
					throw new IllegalStateException("Groq çağrılmamalı");
				})
				.build();
		RequestQuotaService quota = mock(RequestQuotaService.class);
		var service = new ActivitySuggestionServiceImpl(properties, null, null, client, quota);

		ActivitySuggestionException ex = assertThrows(ActivitySuggestionException.class, () -> service.suggest(
				"mersin",
				new ActivitySuggestionRequest(null, null, 2, TravelStyle.BALANCED)
		));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatus());
		assertEquals("ACTIVITY_SUGGESTION_UNAVAILABLE", ex.getCode());
		verify(quota, never()).tryAcquire(QuotaBucket.GROQ);
	}

	@Test
	void hourlyQuotaDoesNotCallGroq() {
		VacationProperties properties = new VacationProperties();
		properties.getGroq().setApiKey("test-key");
		RestClient client = RestClient.builder()
				.requestInterceptor((request, body, execution) -> {
					throw new AssertionError("Groq çağrılmamalı");
				})
				.build();
		RequestQuotaService quota = mock(RequestQuotaService.class);
		when(quota.tryAcquire(QuotaBucket.GROQ)).thenReturn(false);
		DestinationRepository destinations = mock(DestinationRepository.class);
		Destination city = new Destination();
		city.setDisplayName("Mersin");
		when(destinations.findByCityKey(anyString())).thenReturn(Optional.of(city));
		CatalogActivityRepository catalog = mock(CatalogActivityRepository.class);
		when(catalog.findByDestination_CityKey(anyString())).thenReturn(List.of());
		var service = new ActivitySuggestionServiceImpl(properties, destinations, catalog, client, quota);

		QuotaExceededException ex = assertThrows(QuotaExceededException.class, () -> service.suggest(
				"mersin",
				new ActivitySuggestionRequest(null, null, 2, TravelStyle.BALANCED)
		));
		assertEquals("QUOTA_EXCEEDED", ex.getCode());
		assertEquals("Saatte en fazla 30 öneri isteği atabilirsiniz.", ex.getMessage());
	}
}
