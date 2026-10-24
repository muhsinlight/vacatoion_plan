package com.vacationplan.api.service;

import java.util.List;

import com.vacationplan.api.dto.DayPlanDto;
import com.vacationplan.api.model.ActivityOption;
import com.vacationplan.api.service.model.TripPlan;

/**
 * Günlük program taslağı.
 */
public interface ItineraryService {

	/**
	 * Seçilen etkinlikleri günlere böler.
	 *
	 * @param plan seyahat planı
	 * @param activities katalog seçenekleri
	 * @param selectedActivityIds seçilen etkinlik kimlikleri
	 * @return gün planları
	 */
	List<DayPlanDto> build(TripPlan plan, List<ActivityOption> activities, List<String> selectedActivityIds);
}
