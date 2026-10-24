package com.vacationplan.api.service;

import java.util.List;

import com.vacationplan.api.dto.ActivitySuggestionRequest;
import com.vacationplan.api.dto.SuggestedActivityDto;

/**
 * Groq etkinlik önerisi.
 */
public interface ActivitySuggestionService {

	/**
	 * Şehir ve istek için etkinlik önerir.
	 *
	 * @param cityKey il anahtarı
	 * @param request ilgi alanları ve kısıtlar
	 * @return önerilen etkinlikler
	 * @throws com.vacationplan.api.exception.QuotaExceededException saatlik kota doluysa
	 */
	List<SuggestedActivityDto> suggest(String cityKey, ActivitySuggestionRequest request);
}
