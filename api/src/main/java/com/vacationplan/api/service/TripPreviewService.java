package com.vacationplan.api.service;

import java.util.List;

import com.vacationplan.api.dto.BudgetSuggestionDto;
import com.vacationplan.api.dto.BusLegDto;
import com.vacationplan.api.dto.LodgingOfferDto;
import com.vacationplan.api.dto.LodgingPreviewDto;
import com.vacationplan.api.dto.MoneyDto;
import com.vacationplan.api.dto.TripPreviewResponse;
import com.vacationplan.api.dto.scraper.FoodPlaceDto;
import com.vacationplan.api.dto.TripRequest;
import com.vacationplan.api.model.ResolvedBusFare;
import com.vacationplan.api.model.ResolvedLodging;
import com.vacationplan.api.service.model.TripPlanResult;

/**
 * Kayıtsız plan: bütçe, program ve konaklama çözümü.
 */
public interface TripPreviewService {

	/**
	 * İsteği katalog ve kayıtlı fiyatlarla çözer.
	 *
	 * @param request seyahat gövdesi
	 * @return hesaplanmış plan
	 * @throws com.vacationplan.api.exception.DestinationNotFoundException varış kataloğunda yoksa
	 */
	TripPlanResult plan(TripRequest request);

	default TripPreviewResponse preview(TripRequest request) {
		TripPlanResult result = plan(request);
		List<BudgetSuggestionDto> suggestions = result.budgetSuggestions().stream()
				.map(BudgetSuggestionDto::from)
				.toList();
		List<FoodPlaceDto> foodPlaces = result.foodPlaces() == null ? List.of() : result.foodPlaces();
		return new TripPreviewResponse(
				result.assumptions(),
				com.vacationplan.api.dto.BudgetPreviewDto.from(result.budget()),
				result.itinerary(),
				suggestions,
				result.priceNote(),
				toLodgingPreview(result.lodging()),
				foodPlaces,
				busLegs(result.busFare()));
	}

	private static List<BusLegDto> busLegs(ResolvedBusFare fare) {
		if (fare == null || fare.status() != ResolvedBusFare.Status.FRESH || fare.legs() == null) {
			return List.of();
		}
		return fare.legs().stream().map(BusLegDto::from).toList();
	}

	private static LodgingPreviewDto toLodgingPreview(ResolvedLodging lodging) {
		if (lodging == null || lodging.status() == ResolvedLodging.Status.UNAVAILABLE) {
			return null;
		}
		MoneyDto range = lodging.perNightRange() != null
				? MoneyDto.from(lodging.perNightRange())
				: null;
		List<LodgingOfferDto> offers = lodging.offers() == null
				? List.of()
				: lodging.offers().stream().map(LodgingOfferDto::from).toList();
		return new LodgingPreviewDto(
				null,
				range,
				offers,
				lodging.selectedTier()
		);
	}
}
