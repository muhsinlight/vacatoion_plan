package com.vacationplan.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vacationplan.api.dto.ActivityDto;
import com.vacationplan.api.dto.ActivitySuggestionRequest;
import com.vacationplan.api.dto.ActivitySuggestionResponse;
import com.vacationplan.api.dto.DestinationDto;
import com.vacationplan.api.service.ActivitySuggestionService;
import com.vacationplan.api.service.CatalogService;

import jakarta.validation.Valid;

/**
 * Destinasyon kataloğu ve etkinlik önerisi. Taban yol {@code /api/v1/destinations}.
 */
@RestController
@RequestMapping("/api/v1/destinations")
public class DestinationController {

	private final CatalogService catalogService;
	private final ActivitySuggestionService activitySuggestionService;

	public DestinationController(CatalogService catalogService, ActivitySuggestionService activitySuggestionService) {
		this.catalogService = catalogService;
		this.activitySuggestionService = activitySuggestionService;
	}

	/**
	 * Katalogdaki destinasyonlar.
	 *
	 * @return il anahtarı ve görünen ad
	 */
	@GetMapping
	public List<DestinationDto> list() {
		return catalogService.listDestinations();
	}

	/**
	 * Tek destinasyon.
	 *
	 * @param cityKey il anahtarı
	 * @return destinasyon
	 * @throws com.vacationplan.api.exception.DestinationNotFoundException anahtar katalogda yoksa
	 */
	@GetMapping("/{cityKey}")
	public DestinationDto get(@PathVariable String cityKey) {
		return catalogService.getDestination(cityKey);
	}

	/**
	 * Destinasyona bağlı katalog etkinlikleri.
	 *
	 * @param cityKey il anahtarı
	 * @return etkinlik listesi
	 * @throws com.vacationplan.api.exception.DestinationNotFoundException anahtar katalogda yoksa
	 */
	@GetMapping("/{cityKey}/activities")
	public List<ActivityDto> activities(@PathVariable String cityKey) {
		return catalogService.listActivities(cityKey);
	}

	/**
	 * Groq ile etkinlik önerisi ister. Gövde yoksa boş istek kullanılır.
	 *
	 * @param cityKey il anahtarı
	 * @param request ilgi alanları ve kısıtlar; yoksa boş kabul edilir
	 * @return önerilen etkinlikler
	 * @throws com.vacationplan.api.exception.QuotaExceededException saatlik öneri kotası doluysa
	 */
	@PostMapping("/{cityKey}/activity-suggestions")
	public ActivitySuggestionResponse suggest(
			@PathVariable String cityKey,
			@Valid @RequestBody(required = false) ActivitySuggestionRequest request
	) {
		if (request == null) {
			request = new ActivitySuggestionRequest(null, null, null, null);
		}
		return new ActivitySuggestionResponse(activitySuggestionService.suggest(cityKey, request));
	}
}
