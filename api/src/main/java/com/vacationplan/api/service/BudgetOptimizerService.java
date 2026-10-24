package com.vacationplan.api.service;

import java.util.List;

import com.vacationplan.api.model.BudgetBreakdown;
import com.vacationplan.api.model.BudgetSuggestion;
import com.vacationplan.api.model.ResolvedLodging;
import com.vacationplan.api.model.catalog.DestinationProfile;
import com.vacationplan.api.service.model.TripPlan;

/**
 * Bütçe hedefinin üstünde kalan planda kısma önerileri.
 */
public interface BudgetOptimizerService {

	/**
	 * Hedefi aşan kalemler için öneri üretir.
	 *
	 * @param plan seyahat planı
	 * @param profile destinasyon profili
	 * @param breakdown hesaplanmış bütçe
	 * @param lodging çözülmüş konaklama; yoksa {@code null}
	 * @return öneriler
	 */
	List<BudgetSuggestion> suggest(
			TripPlan plan,
			DestinationProfile profile,
			BudgetBreakdown breakdown,
			ResolvedLodging lodging
	);
}
