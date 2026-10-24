package com.vacationplan.api.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.BudgetBreakdown;
import com.vacationplan.api.model.BudgetSuggestion;
import com.vacationplan.api.model.LodgingOffer;
import com.vacationplan.api.model.LodgingTier;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.ResolvedLodging;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.model.catalog.DestinationProfile;
import com.vacationplan.api.service.BudgetOptimizerService;
import com.vacationplan.api.service.CatalogService;
import com.vacationplan.api.service.model.TripPlan;

@Service
public class BudgetOptimizerServiceImpl implements BudgetOptimizerService {

	private final CatalogService catalogService;

	public BudgetOptimizerServiceImpl(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@Override
	public List<BudgetSuggestion> suggest(
			TripPlan plan,
			DestinationProfile profile,
			BudgetBreakdown breakdown,
			ResolvedLodging lodging
	) {
		List<BudgetSuggestion> suggestions = new ArrayList<>();
		BigDecimal limit = plan.totalBudget();
		if (limit == null) {
			return suggestions;
		}
		if (breakdown.total().min().compareTo(limit) <= 0) {
			return suggestions;
		}

		if (plan.travelStyle() != TravelStyle.ECONOMIC) {
			MoneyRange foodSaving = profile.foodPerPersonPerDay()
					.multiply(new BigDecimal("0.20"))
					.multiply(plan.dayCount())
					.multiply(plan.travelerCount());
			suggestions.add(new BudgetSuggestion(
					"TRAVEL_STYLE_ECONOMIC",
					"Seyahat tarzını ekonomik yaparak yemek bütçesini düşürebilirsin.",
					foodSaving.min(),
					foodSaving.max(),
					MoneyRange.TRY
			));
		}

		if (plan.accommodationType() == AccommodationType.HOTEL && plan.nightCount() > 0) {
			int rooms = Math.max(1, (int) Math.ceil(plan.travelerCount() / 2.0));
			LodgingOffer budgetOffer = findTier(lodging, LodgingTier.BUDGET);
			MoneyRange current = selectedLodgingTotal(plan, profile, lodging, rooms);

			if (budgetOffer != null
					&& plan.travelStyle() != TravelStyle.ECONOMIC
					&& budgetOffer.pricePerNight() != null) {
				MoneyRange budgetTotal = MoneyRange.ofTry(budgetOffer.pricePerNight())
						.multiply(plan.nightCount())
						.multiply(rooms);
				MoneyRange diff = saving(current, budgetTotal);
				if (diff.min().compareTo(BigDecimal.ZERO) > 0) {
					suggestions.add(new BudgetSuggestion(
							"LODGING_BUDGET",
							"En ucuz otel seçeneğine geçerek konaklama maliyetini düşürebilirsin: " + budgetOffer.name(),
							diff.min(),
							diff.max(),
							MoneyRange.TRY
					));
				}
			}

			MoneyRange campTotal = catalogService.lodgingQuote(profile, AccommodationType.CAMP)
					.multiply(plan.nightCount())
					.multiply(rooms);
			MoneyRange campDiff = saving(current, campTotal);
			if (campDiff.min().compareTo(BigDecimal.ZERO) > 0) {
				suggestions.add(new BudgetSuggestion(
						"LODGING_CAMP",
						"Otel yerine kamp / düşük maliyetli konaklama düşün.",
						campDiff.min(),
						campDiff.max(),
						MoneyRange.TRY
				));
			}
		}

		return suggestions;
	}

	private static MoneyRange saving(MoneyRange current, MoneyRange cheaper) {
		return MoneyRange.ofTry(
				current.min().subtract(cheaper.max()),
				current.max().subtract(cheaper.min())
		);
	}

	private MoneyRange selectedLodgingTotal(
			TripPlan plan,
			DestinationProfile profile,
			ResolvedLodging lodging,
			int rooms
	) {
		LodgingOffer selected = lodging != null ? lodging.selectedOffer() : null;
		if (selected != null && selected.pricePerNight() != null) {
			return MoneyRange.ofTry(selected.pricePerNight())
					.multiply(plan.nightCount())
					.multiply(rooms);
		}
		return catalogService.lodgingQuote(profile, AccommodationType.HOTEL)
				.multiply(plan.nightCount())
				.multiply(rooms);
	}

	private static LodgingOffer findTier(ResolvedLodging lodging, LodgingTier tier) {
		if (lodging == null || lodging.offers() == null) {
			return null;
		}
		return lodging.offers().stream()
				.filter(offer -> offer.tier() == tier)
				.findFirst()
				.orElse(null);
	}
}
