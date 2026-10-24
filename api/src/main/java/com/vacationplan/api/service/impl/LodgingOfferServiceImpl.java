package com.vacationplan.api.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.dto.scraper.LodgingSearchRequest;
import com.vacationplan.api.dto.scraper.LodgingSearchResponse;
import com.vacationplan.api.dto.scraper.LodgingSearchResponse.ScraperLodgingOffer;
import com.vacationplan.api.entity.LodgingOffer;
import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.LodgingTier;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.QuotaBucket;
import com.vacationplan.api.model.ResolvedLodging;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.repository.LodgingOfferRepository;
import com.vacationplan.api.service.LodgingOfferService;
import com.vacationplan.api.service.RequestQuotaService;
import com.vacationplan.api.util.CityKeyNormalizer;
import com.vacationplan.api.util.ScraperKeys;

@Service
public class LodgingOfferServiceImpl implements LodgingOfferService {

	private static final Logger log = LoggerFactory.getLogger(LodgingOfferServiceImpl.class);

	private final LodgingOfferRepository lodgingOfferRepository;
	private final VacationProperties vacationProperties;
	private final RequestQuotaService requestQuotaService;
	private final RestClient scraperRestClient;
	private final Clock clock;
	private final TransactionOperations transactions;

	public LodgingOfferServiceImpl(
			LodgingOfferRepository lodgingOfferRepository,
			VacationProperties vacationProperties,
			RequestQuotaService requestQuotaService,
			@Qualifier("scraperRestClient") RestClient scraperRestClient,
			Clock clock,
			TransactionOperations transactions
	) {
		this.lodgingOfferRepository = lodgingOfferRepository;
		this.vacationProperties = vacationProperties;
		this.requestQuotaService = requestQuotaService;
		this.scraperRestClient = scraperRestClient;
		this.clock = clock;
		this.transactions = transactions;
	}

	@Override
	public ResolvedLodging resolve(
			String destinationCity,
			LocalDate checkIn,
			LocalDate checkOut,
			int adults,
			AccommodationType accommodationType,
			TravelStyle travelStyle
	) {
		if (accommodationType == null
				|| accommodationType == AccommodationType.NONE
				|| accommodationType == AccommodationType.CAMP
				|| checkIn == null
				|| checkOut == null
				|| !checkOut.isAfter(checkIn)) {
			return ResolvedLodging.unavailable();
		}

		String cityKey = CityKeyNormalizer.normalize(destinationCity);
		if (cityKey.isBlank()) {
			return ResolvedLodging.unavailable();
		}
		LodgingTier selectedTier = LodgingTier.forTravelStyle(travelStyle);
		List<LodgingOffer> cached = lodgingOfferRepository
				.findByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
						cityKey, accommodationType, checkIn, checkOut);
		if (!isFresh(cached)) {
			fetchAndStore(cityKey, checkIn, checkOut, adults, accommodationType);
			cached = lodgingOfferRepository.findByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
					cityKey, accommodationType, checkIn, checkOut);
		}
		if (!isFresh(cached)) {
			return ResolvedLodging.unavailable();
		}

		cached = distinctNames(cached);
		List<com.vacationplan.api.model.LodgingOffer> offers = cached.stream().map(this::toOffer).toList();
		return new ResolvedLodging(
				cached.get(0).getProvider(),
				bandFrom(cached),
				offers,
				selectedTier,
				ResolvedLodging.Status.FRESH,
				cached.stream().allMatch(offer -> offer != null && offer.isEstimated())
		);
	}

	private List<LodgingOffer> storeLiveOffers(
			String cityKey,
			AccommodationType accommodationType,
			LocalDate checkIn,
			LocalDate checkOut,
			List<LodgingOffer> offers
	) {
		lodgingOfferRepository.deleteByCityKeyAndAccommodationTypeAndCheckInAndCheckOut(
				cityKey, accommodationType, checkIn, checkOut);
		return lodgingOfferRepository.saveAll(offers);
	}

	private void fetchAndStore(
			String cityKey,
			LocalDate checkIn,
			LocalDate checkOut,
			int adults,
			AccommodationType accommodationType
	) {
		List<LodgingOffer> fetched = fetchLiveOffers(cityKey, checkIn, checkOut, adults, accommodationType);
		if (!fetched.isEmpty()) {
			transactions.executeWithoutResult(status -> {
				storeLiveOffers(cityKey, accommodationType, checkIn, checkOut, fetched);
			});
		}
	}

	private List<LodgingOffer> fetchLiveOffers(
			String cityKey,
			LocalDate checkIn,
			LocalDate checkOut,
			int adults,
			AccommodationType accommodationType
	) {
		if (!ScraperKeys.isUsable(vacationProperties.getScraperTriggerKey())) {
			log.warn("Konaklama fiyatı atlandı: scraper anahtarı yok");
			return List.of();
		}
		if (!requestQuotaService.tryAcquire(QuotaBucket.SCRAPER)) {
			log.warn("Konaklama fiyatı atlandı: saatlik scraper kotası doldu");
			return List.of();
		}
		try {
			LodgingSearchResponse response = scraperRestClient.post()
					.uri("/lodging/search")
					.header(ScraperKeys.HEADER, vacationProperties.getScraperTriggerKey())
					.body(new LodgingSearchRequest(
							cityKey,
							checkIn,
							checkOut,
							Math.max(1, adults),
							accommodationType
					))
					.retrieve()
					.body(LodgingSearchResponse.class);
			if (response == null || response.offers() == null || response.offers().isEmpty()) {
				if (response != null && !response.errors().isEmpty()) {
					log.warn("Konaklama fiyatı alınamadı: {}", response.errors());
				}
				return List.of();
			}
			Map<LodgingTier, LodgingOffer> byTier = new LinkedHashMap<>();
			for (ScraperLodgingOffer offer : response.offers()) {
				LodgingOffer entity = toEntity(cityKey, accommodationType, checkIn, checkOut, response, offer);
				if (entity != null) {
					byTier.putIfAbsent(entity.getTier(), entity);
				}
			}
			if (byTier.isEmpty()) {
				return List.of();
			}
			return distinctNames(new ArrayList<>(byTier.values()));
		} catch (RestClientException ex) {
			log.warn("Konaklama fiyatı alınamadı: {}", cityKey);
			return List.of();
		}
	}

	private LodgingOffer toEntity(
			String cityKey,
			AccommodationType accommodationType,
			LocalDate checkIn,
			LocalDate checkOut,
			LodgingSearchResponse response,
			ScraperLodgingOffer offer
	) {
		if (offer.tier() == null || offer.name() == null || offer.name().isBlank() || offer.pricePerNight() == null) {
			return null;
		}
		LodgingOffer entity = new LodgingOffer();
		entity.setCityKey(cityKey);
		entity.setProvider(limit(firstText(offer.provider(), response.provider(), "obilet"), 40));
		entity.setAccommodationType(accommodationType);
		entity.setCheckIn(checkIn);
		entity.setCheckOut(checkOut);
		entity.setTier(offer.tier());
		entity.setName(limit(offer.name().strip(), 200));
		entity.setStars(offer.stars());
		entity.setRating(offer.rating());
		entity.setBoardType(limit(offer.boardType(), 80));
		entity.setFeatures(limit(features(offer.features()), 500));
		entity.setPricePerNight(offer.pricePerNight());
		entity.setUrl(limit(offer.url(), 500));
		entity.setImageUrl(limit(offer.imageUrl(), 500));
		if (response.band() != null) {
			entity.setBandMin(response.band().min());
			entity.setBandTypical(response.band().typical());
			entity.setBandMax(response.band().max());
		}
		entity.setCheckedAt(clock.instant());
		entity.setEstimated(Boolean.TRUE.equals(response.estimated()));
		return entity;
	}

	private static String firstText(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value.strip();
			}
		}
		return "obilet";
	}

	private static String features(List<String> features) {
		if (features == null || features.isEmpty()) {
			return null;
		}
		return String.join(", ", features);
	}

	private static String limit(String value, int max) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.strip();
		return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
	}

	private boolean isFresh(List<LodgingOffer> offers) {
		if (offers == null || offers.isEmpty()) {
			return false;
		}
		if (hasDuplicateNames(offers)) {
			return false;
		}
		Duration maxAge = vacationProperties.getLodgingQuoteMaxAge();
		Instant cutoff = clock.instant().minus(maxAge);
		return offers.stream().allMatch(offer -> offer.getCheckedAt() != null && offer.getCheckedAt().isAfter(cutoff));
	}

	private static List<LodgingOffer> distinctNames(List<LodgingOffer> offers) {
		Map<String, LodgingOffer> byName = new LinkedHashMap<>();
		offers.stream()
				.sorted(Comparator.comparing(offer -> offer != null ? offer.getTier() : null))
				.forEach(offer -> {
					String name = offer.getName() == null ? "" : offer.getName().strip().toLowerCase(Locale.ROOT);
					byName.putIfAbsent(name, offer);
				});
		return new ArrayList<>(byName.values());
	}

	private static boolean hasDuplicateNames(List<LodgingOffer> offers) {
		return distinctNames(offers).size() < offers.size();
	}

	private MoneyRange bandFrom(List<LodgingOffer> offers) {
		LodgingOffer first = offers.get(0);
		if (first.getBandMin() != null && first.getBandMax() != null) {
			return MoneyRange.ofTry(first.getBandMin(), first.getBandMax());
		}
		BigDecimal min = offers.stream()
				.map(offer -> offer.getPricePerNight())
				.min(Comparator.naturalOrder())
				.orElse(BigDecimal.ZERO);
		BigDecimal max = offers.stream()
				.map(offer -> offer.getPricePerNight())
				.max(Comparator.naturalOrder())
				.orElse(min);
		return MoneyRange.ofTry(min, max);
	}

	private com.vacationplan.api.model.LodgingOffer toOffer(LodgingOffer entity) {
		List<String> features = List.of();
		if (entity.getFeatures() != null && !entity.getFeatures().isBlank()) {
			features = List.of(entity.getFeatures().split("\\s*,\\s*"));
		}
		return new com.vacationplan.api.model.LodgingOffer(
				entity.getTier(),
				entity.getProvider(),
				entity.getName(),
				entity.getStars(),
				entity.getRating(),
				entity.getBoardType(),
				features,
				entity.getPricePerNight(),
				entity.getUrl(),
				entity.getImageUrl()
		);
	}
}
