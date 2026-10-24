package com.vacationplan.api.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.vacationplan.api.dto.Assumption;
import com.vacationplan.api.dto.TripRequest;
import com.vacationplan.api.dto.scraper.FoodPlaceDto;
import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.ActivityOption;
import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.BudgetLineItem;
import com.vacationplan.api.model.LodgingOffer;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.model.ResolvedBusFare;
import com.vacationplan.api.model.ResolvedCarFuel;
import com.vacationplan.api.model.ResolvedLodging;
import com.vacationplan.api.model.TransportMode;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.model.catalog.DestinationProfile;
import com.vacationplan.api.service.BudgetCalculationService;
import com.vacationplan.api.service.CarFuelQuoteService;
import com.vacationplan.api.service.BudgetOptimizerService;
import com.vacationplan.api.service.BusRouteFareService;
import com.vacationplan.api.service.CatalogService;
import com.vacationplan.api.service.FoodQuoteService;
import com.vacationplan.api.service.ItineraryService;
import com.vacationplan.api.service.LodgingOfferService;
import com.vacationplan.api.service.TripPreviewService;
import com.vacationplan.api.service.model.TripPlan;
import com.vacationplan.api.service.model.TripPlanResult;
import com.vacationplan.api.service.model.TripPlans;

@Service
public class TripPreviewServiceImpl implements TripPreviewService {

	private final CatalogService catalogService;
	private final BudgetCalculationService budgetCalculationService;
	private final ItineraryService itineraryService;
	private final BudgetOptimizerService budgetOptimizerService;
	private final BusRouteFareService busRouteFareService;
	private final LodgingOfferService lodgingOfferService;
	private final FoodQuoteService foodQuoteService;
	private final CarFuelQuoteService carFuelQuoteService;

	public TripPreviewServiceImpl(
			CatalogService catalogService,
			BudgetCalculationService budgetCalculationService,
			ItineraryService itineraryService,
			BudgetOptimizerService budgetOptimizerService,
			BusRouteFareService busRouteFareService,
			LodgingOfferService lodgingOfferService,
			FoodQuoteService foodQuoteService,
			CarFuelQuoteService carFuelQuoteService
	) {
		this.catalogService = catalogService;
		this.budgetCalculationService = budgetCalculationService;
		this.itineraryService = itineraryService;
		this.budgetOptimizerService = budgetOptimizerService;
		this.busRouteFareService = busRouteFareService;
		this.lodgingOfferService = lodgingOfferService;
		this.foodQuoteService = foodQuoteService;
		this.carFuelQuoteService = carFuelQuoteService;
	}

	@Override
	public TripPlanResult plan(TripRequest request) {
		List<Assumption> assumptions = new ArrayList<>();
		noteDefaults(request, assumptions);
		TripPlan plan = TripPlans.from(request);

		DestinationProfile profile = catalogService.resolveForTrip(plan.destinationCity());
		FoodQuoteService.Result foodRefresh = foodQuoteService.refreshIfStale(profile.cityKey());
		if (foodRefresh == FoodQuoteService.Result.UPDATED) {
			profile = catalogService.resolveForTrip(profile.cityKey());
		}
		if (foodRefresh == FoodQuoteService.Result.FAILED || foodRefresh == FoodQuoteService.Result.SCHEDULED) {
			assumptions.add(new Assumption(
					"food",
					foodRefresh == FoodQuoteService.Result.SCHEDULED
							? "Yeme-içme katalog tahmininden yazıldı; canlı liste arka planda güncelleniyor."
							: "Canlı restoran fiyatı alınamadı; yeme-içme katalog tahmininden yazıldı."
			));
		}
		List<ActivityOption> activities = profile.activities();
		Set<String> selectedIds = resolveSelectedActivityIds(plan, activities, assumptions);

		ResolvedBusFare busFare = null;
		ResolvedCarFuel carFuel = null;
		if (plan.transportMode() == TransportMode.CAR) {
			carFuel = carFuelQuoteService.quote(plan.travelStyle());
			assumptions.add(new Assumption("transportMode", carFuel.note()));
		}
		if (plan.transportMode() == TransportMode.BUS) {
			busFare = busRouteFareService.roundTrip(
					plan.originCity(),
					plan.destinationCity(),
					plan.stay().start(),
					plan.stay().end(),
					budgetCalculationService.adultEquivalent(plan)
			);
			if (busFare.status() == ResolvedBusFare.Status.UNAVAILABLE) {
				assumptions.add(new Assumption(
						"transportMode",
						busFare.reason() != null
								? busFare.reason()
								: "Canlı otobüs fiyatı alınamadı; ulaşım katalog tahmininden yazıldı."
				));
			}
		}

		ResolvedLodging lodging = lodgingOfferService.resolve(
				plan.destinationCity(),
				plan.stay().start(),
				plan.stay().end(),
				plan.adults(),
				plan.accommodationType(),
				plan.travelStyle()
		);
		if (needsLiveLodging(plan.accommodationType()) && plan.nightCount() > 0) {
			if (lodging.cityRange()) {
				assumptions.add(new Assumption(
						"accommodationType",
						"Konaklama Obilet şehir aralığından yazıldı; seçilen odanın gecelik fiyatı değil."
				));
			} else if (lodging.status() == ResolvedLodging.Status.UNAVAILABLE) {
				assumptions.add(new Assumption(
						"accommodationType",
						"Canlı konaklama fiyatı alınamadı; katalog tahmini kullanıldı."
				));
			}
		}

		List<FoodPlaceDto> foodPlaces = new ArrayList<>();
		List<BudgetLineItem> lines = buildLineItems(
				plan, profile, activities, selectedIds, busFare, carFuel, lodging, foodPlaces);
		var breakdown = budgetCalculationService.calculate(plan, lines);
		appendBudgetWarnings(plan, breakdown, assumptions);

		String priceNote = priceNote(
				busFare,
				carFuel,
				lodging,
				profile.foodSourceType() == PriceSourceType.OFFICIAL_API);
		var itinerary = itineraryService.build(plan, activities, List.copyOf(selectedIds));
		var suggestions = budgetOptimizerService.suggest(plan, profile, breakdown, lodging);

		return new TripPlanResult(
				plan, assumptions, breakdown, itinerary, suggestions, priceNote, lodging, List.copyOf(foodPlaces), busFare);
	}

	private static void noteDefaults(TripRequest request, List<Assumption> assumptions) {
		if (request.transportMode() == null) {
			assumptions.add(new Assumption("transportMode", "Ulaşım belirtilmedi; otobüs varsayıldı."));
		}
		if (request.accommodationType() == null) {
			assumptions.add(new Assumption("accommodationType", "Konaklama belirtilmedi; varsayılan uygulandı."));
		}
		if (request.travelStyle() == null) {
			assumptions.add(new Assumption("travelStyle", "Seyahat tarzı belirtilmedi; dengeli varsayıldı."));
		}
	}

	private Set<String> resolveSelectedActivityIds(
			TripPlan plan,
			List<ActivityOption> activities,
			List<Assumption> assumptions
	) {
		if (plan.selectedActivityIds() != null) {
			return new HashSet<>(plan.selectedActivityIds());
		}
		assumptions.add(new Assumption(
				"selectedActivityIds",
				"Aktivite seçilmedi; varsayılan öneriler bütçeye eklendi."
		));
		Set<String> defaults = new HashSet<>();
		for (ActivityOption activity : activities) {
			if (activity.defaultSelected()) {
				defaults.add(activity.id());
			}
		}
		return defaults;
	}

	private static String priceNote(
			ResolvedBusFare busFare,
			ResolvedCarFuel carFuel,
			ResolvedLodging lodging,
			boolean foodLive
	) {
		boolean busLive = busFare != null && busFare.status() == ResolvedBusFare.Status.FRESH;
		boolean carLive = carFuel != null && carFuel.status() == ResolvedCarFuel.Status.FRESH;
		boolean lodgingLive = lodging != null
				&& lodging.status() == ResolvedLodging.Status.FRESH;
		List<String> live = new ArrayList<>();
		if (busLive) {
			live.add("Otobüs");
		}
		if (carLive) {
			live.add("Araç yakıtı");
		}
		if (lodgingLive) {
			live.add("Konaklama");
		}
		if (foodLive) {
			live.add("Yeme-içme");
		}
		if (live.isEmpty()) {
			return "Fiyatlar veritabanı kataloğundan.";
		}
		return String.join(", ", live) + " güncel fiyat bandından. Diğer kalemler katalog tahmininden.";
	}

	private List<BudgetLineItem> buildLineItems(
			TripPlan plan,
			DestinationProfile profile,
			List<ActivityOption> activities,
			Set<String> selectedIds,
			ResolvedBusFare busFare,
			ResolvedCarFuel carFuel,
			ResolvedLodging lodging,
			List<FoodPlaceDto> foodPlaces
	) {
		List<BudgetLineItem> lines = new ArrayList<>();

		if (carFuel != null && carFuel.amount() != null && carFuel.status() == ResolvedCarFuel.Status.FRESH) {
			lines.add(new BudgetLineItem(
					BudgetCategory.TRANSPORT,
					"Araç yakıtı",
					carFuel.amount(),
					PriceSourceType.SCRAPE,
					true
			));
		} else if (busFare != null && busFare.amount() != null && busFare.status() == ResolvedBusFare.Status.FRESH) {
			lines.add(new BudgetLineItem(
					BudgetCategory.TRANSPORT,
					"Gidiş-dönüş otobüs",
					busFare.amount(),
					PriceSourceType.SCRAPE,
					true
			));
		} else {
			MoneyRange transportPerAdult = catalogService.transportQuote(profile, plan.transportMode(), plan.travelStyle());
			boolean oneVehicle = plan.transportMode() == TransportMode.CAR;
			var travelers = oneVehicle ? BigDecimal.ONE : budgetCalculationService.adultEquivalent(plan);
			lines.add(new BudgetLineItem(
					BudgetCategory.TRANSPORT,
					oneVehicle ? "Gidiş-dönüş araç" : "Gidiş-dönüş ulaşım",
					transportPerAdult.multiply(travelers),
					PriceSourceType.ESTIMATE,
					true
			));
		}

		int nights = plan.nightCount();
		if (nights > 0 && plan.accommodationType() != AccommodationType.NONE) {
			int rooms = Math.max(1, (int) Math.ceil(plan.travelerCount() / 2.0));
			LodgingOffer selected = lodging != null && lodging.status() == ResolvedLodging.Status.FRESH
					? lodging.selectedOffer()
					: null;
			if (selected != null && selected.pricePerNight() != null) {
				boolean cityRange = lodging != null && lodging.cityRange();
				MoneyRange perNight = cityRange && lodging != null && lodging.perNightRange() != null
						? lodging.perNightRange()
						: MoneyRange.ofTry(selected.pricePerNight());
				String label = cityRange
						? "Konaklama şehir aralığı (" + nights + " gece, " + rooms + " oda)"
						: "Konaklama - " + selected.name() + " (" + nights + " gece, " + rooms + " oda)";
				lines.add(new BudgetLineItem(
						BudgetCategory.LODGING,
						label,
						perNight.multiply(nights).multiply(rooms),
						PriceSourceType.SCRAPE,
						true
				));
			} else {
				MoneyRange perNight = catalogService.lodgingQuote(profile, plan.accommodationType());
				lines.add(new BudgetLineItem(
						BudgetCategory.LODGING,
						"Konaklama (" + nights + " gece, " + rooms + " oda)",
						perNight.multiply(nights).multiply(rooms),
						PriceSourceType.ESTIMATE,
						true
				));
			}
		}

		MoneyRange foodBase = profile.foodPerPersonPerDay().multiply(plan.travelStyle().foodMultiplier());
		boolean foodLive = profile.foodSourceType() == PriceSourceType.OFFICIAL_API;
		foodPlaces.addAll(profile.foodPlaces());
		lines.add(new BudgetLineItem(
				BudgetCategory.FOOD,
				foodLive ? "Yeme-içme (yüksek puanlı restoranlar)" : "Yeme-içme",
				foodBase.multiply(plan.dayCount()).multiply(plan.travelerCount()),
				foodLive ? PriceSourceType.OFFICIAL_API : PriceSourceType.ESTIMATE,
				true
		));

		int days = Math.max(plan.dayCount(), 1);
		boolean driving = plan.transportMode() == TransportMode.CAR;
		BigDecimal heads = driving ? BigDecimal.ONE : budgetCalculationService.adultEquivalent(plan);
		String localLabel = driving
				? "Şehir içi ulaşım (" + days + " gün, araçla)"
				: "Şehir içi ulaşım (" + days + " gün, kişi başı)";
		lines.add(new BudgetLineItem(
				BudgetCategory.LOCAL_TRANSPORT,
				localLabel,
				localDaily(profile.localTransportPerDay(), plan.travelStyle()).multiply(days).multiply(heads),
				PriceSourceType.ESTIMATE,
				true
		));

		for (ActivityOption activity : activities) {
			boolean selected = selectedIds.contains(activity.id());
			lines.add(new BudgetLineItem(
					BudgetCategory.ACTIVITY,
					activity.name(),
					activity.entryFeePerPerson().multiply(plan.travelerCount()),
					activity.sourceType(),
					selected
			));
		}

		return lines;
	}

	private static MoneyRange localDaily(MoneyRange perDay, TravelStyle style) {
		if (style == TravelStyle.ECONOMIC) {
			return perDay.multiply(new BigDecimal("0.90"));
		}
		if (style == TravelStyle.COMFORT) {
			return perDay.multiply(new BigDecimal("1.15"));
		}
		return perDay;
	}

	private static boolean needsLiveLodging(AccommodationType type) {
		return type == AccommodationType.HOTEL
				|| type == AccommodationType.PENSION
				|| type == AccommodationType.RENTAL;
	}

	private static void appendBudgetWarnings(
			TripPlan plan,
			com.vacationplan.api.model.BudgetBreakdown breakdown,
			List<Assumption> assumptions
	) {
		if (plan.totalBudget() != null
				&& breakdown.total().min().compareTo(plan.totalBudget()) > 0) {
			assumptions.add(new Assumption(
					"totalBudget",
					"Tahmini minimum tutar (" + breakdown.total().min() + " TRY) toplam bütçenin üzerinde."
			));
		}
		if (plan.perPersonBudget() != null
				&& breakdown.perPerson().min().compareTo(plan.perPersonBudget()) > 0) {
			assumptions.add(new Assumption(
					"perPersonBudget",
					"Tahmini kişi başı minimum (" + breakdown.perPerson().min() + " TRY) hedefin üzerinde."
			));
		}
	}
}
