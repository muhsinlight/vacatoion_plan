package com.vacationplan.api.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.dto.ActivitySuggestionRequest;
import com.vacationplan.api.dto.SuggestedActivityDto;
import com.vacationplan.api.exception.ActivitySuggestionException;
import com.vacationplan.api.exception.DestinationNotFoundException;
import com.vacationplan.api.exception.QuotaExceededException;
import com.vacationplan.api.model.QuotaBucket;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.repository.CatalogActivityRepository;
import com.vacationplan.api.repository.DestinationRepository;
import com.vacationplan.api.service.ActivitySuggestionParser;
import com.vacationplan.api.service.ActivitySuggestionService;
import com.vacationplan.api.service.RequestQuotaService;
import com.vacationplan.api.util.CityKeyNormalizer;

@Service
public class ActivitySuggestionServiceImpl implements ActivitySuggestionService {

	private final VacationProperties properties;
	private final DestinationRepository destinationRepository;
	private final CatalogActivityRepository catalogActivityRepository;
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final RestClient groqRestClient;
	private final RequestQuotaService requestQuotaService;
	private final String systemPrompt;

	public ActivitySuggestionServiceImpl(
			VacationProperties properties,
			DestinationRepository destinationRepository,
			CatalogActivityRepository catalogActivityRepository,
			RestClient groqRestClient,
			RequestQuotaService requestQuotaService
	) {
		this.properties = properties;
		this.destinationRepository = destinationRepository;
		this.catalogActivityRepository = catalogActivityRepository;
		this.groqRestClient = groqRestClient;
		this.requestQuotaService = requestQuotaService;
		try {
			this.systemPrompt = new ClassPathResource("prompts/activity-suggest.txt")
					.getContentAsString(StandardCharsets.UTF_8)
					.trim();
		} catch (Exception ex) {
			throw ActivitySuggestionException.promptUnreadable(ex);
		}
	}

	@Override
	public List<SuggestedActivityDto> suggest(String cityKey, ActivitySuggestionRequest request) {
		String apiKey = properties.getGroq().getApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			throw ActivitySuggestionException.missingApiKey();
		}
		String normalized = CityKeyNormalizer.normalize(cityKey);
		String cityName = destinationRepository.findByCityKey(normalized)
				.map(destination -> destination.getDisplayName())
				.orElseThrow(() -> new DestinationNotFoundException(cityKey));
		if (!requestQuotaService.tryAcquire(QuotaBucket.GROQ)) {
			throw QuotaExceededException.hourly(properties.getQuota().getGroqMaxPerHour(), "öneri");
		}
		List<String> existing = catalogActivityRepository.findByDestination_CityKey(normalized).stream()
				.map(activity -> activity.getName())
				.toList();
		try {
			String raw = groqRestClient.post()
					.uri("/chat/completions")
					.header("Authorization", "Bearer " + apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of(
							"model", properties.getGroq().getModel(),
							"messages", List.of(
									Map.of("role", "system", "content", systemPrompt),
									Map.of("role", "user", "content", userMessage(request, cityName, existing))
							)
					))
					.retrieve()
					.body(String.class);
			return ActivitySuggestionParser.parse(messageContent(raw));
		} catch (RestClientException ex) {
			throw ActivitySuggestionException.upstreamFailed();
		}
	}

	private static String messageContent(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			JsonNode content = MAPPER.readTree(raw).path("choices").path(0).path("message").path("content");
			if (content.isMissingNode() || content.isNull()) {
				return null;
			}
			return content.asText(null);
		} catch (Exception ex) {
			throw ActivitySuggestionException.unreadable();
		}
	}

	private static String userMessage(ActivitySuggestionRequest request, String cityName, List<String> existing) {
		int adults = request.adults() == null ? 1 : request.adults();
		String dates = request.startDate() == null || request.endDate() == null
				? "belirtilmedi"
				: request.startDate() + " – " + request.endDate();
		String listed = existing.isEmpty() ? "yok" : String.join(", ", existing);
		return """
				Şehir: %s
				Tarih: %s
				Kişi: %d
				Stil: %s
				Zaten listede olanlar: %s
				""".formatted(cityName, dates, adults, styleLabel(request.travelStyle()), listed);
	}

	private static String styleLabel(TravelStyle style) {
		if (style == null) {
			return "belirtilmedi";
		}
		return switch (style) {
			case ECONOMIC -> "ekonomik";
			case BALANCED -> "dengeli";
			case COMFORT -> "konfor";
		};
	}
}
