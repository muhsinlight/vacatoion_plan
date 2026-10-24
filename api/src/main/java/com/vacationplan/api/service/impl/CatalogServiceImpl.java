package com.vacationplan.api.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vacationplan.api.dto.ActivityDto;
import com.vacationplan.api.dto.DestinationDto;
import com.vacationplan.api.dto.scraper.FoodPlaceDto;
import com.vacationplan.api.entity.CatalogActivity;
import com.vacationplan.api.entity.Destination;
import com.vacationplan.api.exception.DestinationNotFoundException;
import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.ActivityOption;
import com.vacationplan.api.model.MoneyRange;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.model.TransportMode;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.model.catalog.DestinationProfile;
import com.vacationplan.api.repository.DestinationRepository;
import com.vacationplan.api.service.CatalogService;
import com.vacationplan.api.util.CityKeyNormalizer;
import com.vacationplan.api.util.FoodPlacesJson;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

	private final DestinationRepository destinationRepository;

	public CatalogServiceImpl(DestinationRepository destinationRepository) {
		this.destinationRepository = destinationRepository;
	}

	@Override
	public List<DestinationDto> listDestinations() {
		return destinationRepository.findAll().stream()
				.map(d -> new DestinationDto(d.getCityKey(), d.getDisplayName()))
				.toList();
	}

	@Override
	public DestinationDto getDestination(String cityKey) {
		Destination destination = destinationRepository.findByCityKey(CityKeyNormalizer.normalize(cityKey))
				.orElseThrow(() -> new DestinationNotFoundException(cityKey));
		return new DestinationDto(destination.getCityKey(), destination.getDisplayName());
	}

	@Override
	public List<ActivityDto> listActivities(String cityKey) {
		return destinationRepository.findByCityKey(CityKeyNormalizer.normalize(cityKey))
				.map(destination -> destination.getActivities().stream().map(this::toActivityDto).toList())
				.orElseThrow(() -> new DestinationNotFoundException(cityKey));
	}

	@Override
	public DestinationProfile resolveForTrip(String destinationCity) {
		String key = CityKeyNormalizer.normalize(destinationCity);
		return destinationRepository.findByCityKey(key)
				.map(this::toProfile)
				.orElseThrow(() -> new DestinationNotFoundException(destinationCity));
	}

	@Override
	public MoneyRange transportQuote(DestinationProfile profile, TransportMode mode, TravelStyle style) {
		MoneyRange base = switch (mode == null ? TransportMode.BUS : mode) {
			case BUS, OTHER -> profile.busRoundTripPerAdult();
			case TRAIN -> profile.trainRoundTripPerAdult();
			case PLANE -> profile.planeRoundTripPerAdult();
			case CAR -> profile.carRoundTripPerAdult();
		};
		if (style == TravelStyle.COMFORT && mode != TransportMode.CAR) {
			return base.multiply(new BigDecimal("1.15"));
		}
		if (style == TravelStyle.ECONOMIC) {
			return base.multiply(new BigDecimal("0.90"));
		}
		return base;
	}

	@Override
	public MoneyRange lodgingQuote(DestinationProfile profile, AccommodationType type) {
		if (type == null || type == AccommodationType.NONE) {
			return MoneyRange.zeroTry();
		}
		return switch (type) {
			case HOTEL -> profile.lodgingPerNight();
			case PENSION -> profile.lodgingPerNight().multiply(new BigDecimal("0.70"));
			case RENTAL -> profile.lodgingPerNight().multiply(new BigDecimal("1.10"));
			case CAMP -> profile.lodgingPerNight().multiply(new BigDecimal("0.35"));
			case NONE -> MoneyRange.zeroTry();
		};
	}

	private DestinationProfile toProfile(Destination destination) {
		List<ActivityOption> activities = destination.getActivities().stream()
				.map(this::toActivityOption)
				.toList();
		PriceSourceType foodSource = destination.getFoodSourceType() == null
				? PriceSourceType.ESTIMATE
				: destination.getFoodSourceType();
		List<FoodPlaceDto> foodPlaces = FoodPlacesJson.read(destination.getFoodPlacesJson());
		return new DestinationProfile(
				destination.getCityKey(),
				destination.getDisplayName(),
				MoneyRange.ofTry(destination.getBusRoundTripMin(), destination.getBusRoundTripMax()),
				MoneyRange.ofTry(destination.getTrainRoundTripMin(), destination.getTrainRoundTripMax()),
				MoneyRange.ofTry(destination.getPlaneRoundTripMin(), destination.getPlaneRoundTripMax()),
				MoneyRange.ofTry(destination.getCarRoundTripMin(), destination.getCarRoundTripMax()),
				MoneyRange.ofTry(destination.getLodgingPerNightMin(), destination.getLodgingPerNightMax()),
				MoneyRange.ofTry(destination.getFoodPerPersonPerDayMin(), destination.getFoodPerPersonPerDayMax()),
				foodSource,
				MoneyRange.ofTry(destination.getLocalTransportPerDayMin(), destination.getLocalTransportPerDayMax()),
				activities,
				foodPlaces
		);
	}

	private ActivityOption toActivityOption(CatalogActivity activity) {
		return new ActivityOption(
				activity.getActivityKey(),
				activity.getName(),
				activity.getDescription(),
				activity.getEstimatedMinutes(),
				MoneyRange.ofTry(activity.getEntryFeeMin(), activity.getEntryFeeMax()),
				activity.getSourceType(),
				activity.getSourceUrl(),
				activity.isDefaultSelected()
		);
	}

	private ActivityDto toActivityDto(CatalogActivity activity) {
		return ActivityDto.from(toActivityOption(activity));
	}
}
