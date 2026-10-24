package com.vacationplan.api.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.dto.scraper.PriceObservationBatchRequest;
import com.vacationplan.api.dto.scraper.PriceObservationBatchResponse;
import com.vacationplan.api.dto.scraper.PriceObservationDto;
import com.vacationplan.api.entity.FuelPrice;
import com.vacationplan.api.entity.PriceObservation;
import com.vacationplan.api.exception.ScrapeException;
import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.FuelType;
import com.vacationplan.api.model.PriceScopeType;
import com.vacationplan.api.model.PriceUnit;
import com.vacationplan.api.repository.DestinationRepository;
import com.vacationplan.api.repository.FuelPriceRepository;
import com.vacationplan.api.repository.PriceObservationRepository;
import com.vacationplan.api.service.PriceObservationIngestService;
import com.vacationplan.api.util.CityKeyNormalizer;

@Service
public class PriceObservationIngestServiceImpl implements PriceObservationIngestService {

	private final PriceObservationRepository priceObservationRepository;
	private final FuelPriceRepository fuelPriceRepository;
	private final DestinationRepository destinationRepository;
	private final RouteQuoteMaterializer routeQuoteMaterializer;
	private final VacationProperties vacationProperties;
	private final Clock clock;

	public PriceObservationIngestServiceImpl(
			PriceObservationRepository priceObservationRepository,
			FuelPriceRepository fuelPriceRepository,
			DestinationRepository destinationRepository,
			RouteQuoteMaterializer routeQuoteMaterializer,
			VacationProperties vacationProperties,
			Clock clock
	) {
		this.priceObservationRepository = priceObservationRepository;
		this.fuelPriceRepository = fuelPriceRepository;
		this.destinationRepository = destinationRepository;
		this.routeQuoteMaterializer = routeQuoteMaterializer;
		this.vacationProperties = vacationProperties;
		this.clock = clock;
	}

	@Override
	@Transactional
	public PriceObservationBatchResponse ingestBatch(PriceObservationBatchRequest request) {
		pruneOldObservations();

		List<Long> ids = new ArrayList<>();
		int routeUpdates = 0;

		for (PriceObservationDto dto : request.observations()) {
			validateBand(dto);
			PriceObservation entity = saveObservation(dto);
			ids.add(entity.getId());

			if (routeQuoteMaterializer.materialize(dto, entity)) {
				routeUpdates++;
			}
			if (dto.scopeType() == PriceScopeType.NATIONAL_FUEL) {
				upsertFuelPrice(dto, entity.getCheckedAt());
			}
			if (isFoodDestination(dto)) {
				applyFoodBand(dto, entity.getCheckedAt());
			}
		}

		return new PriceObservationBatchResponse(ids.size(), routeUpdates, ids);
	}

	private void pruneOldObservations() {
		var retention = vacationProperties.getPriceObservationRetention();
		if (retention == null || retention.isZero() || retention.isNegative()) {
			return;
		}
		Instant cutoff = clock.instant().minus(retention);
		priceObservationRepository.deleteByCheckedAtBefore(cutoff);
	}

	private PriceObservation saveObservation(PriceObservationDto dto) {
		Instant checkedAt = dto.checkedAt() != null ? dto.checkedAt() : clock.instant();
		LocalDate observationDay = checkedAt.atZone(ZoneOffset.UTC).toLocalDate();
		PriceObservation entity = findExisting(dto, observationDay).orElseGet(PriceObservation::new);
		applyDto(entity, dto, checkedAt, observationDay);
		return priceObservationRepository.save(entity);
	}

	private Optional<PriceObservation> findExisting(PriceObservationDto dto, LocalDate observationDay) {
		if (dto.weekBucket() != null) {
			return priceObservationRepository.findByScopeTypeAndScopeKeyAndUnitAndObservationDayAndWeekBucket(
					dto.scopeType(), dto.scopeKey(), dto.unit(), observationDay, dto.weekBucket());
		}
		return priceObservationRepository.findByScopeTypeAndScopeKeyAndUnitAndObservationDayAndWeekBucketIsNull(
				dto.scopeType(), dto.scopeKey(), dto.unit(), observationDay);
	}

	private void applyDto(
			PriceObservation entity,
			PriceObservationDto dto,
			Instant checkedAt,
			LocalDate observationDay
	) {
		entity.setCategory(dto.category());
		entity.setScopeType(dto.scopeType());
		entity.setScopeKey(destinationScopeKey(dto));
		entity.setUnit(dto.unit());
		entity.setMinAmount(dto.min());
		entity.setTypicalAmount(dto.typical());
		entity.setMaxAmount(dto.max());
		entity.setCurrency(dto.currency());
		entity.setSourceType(dto.sourceType());
		entity.setProvider(dto.provider());
		entity.setSourceUrl(dto.sourceUrl());
		entity.setSampleSize(dto.sampleSize());
		entity.setConfidence(dto.confidence());
		entity.setCheckedAt(checkedAt);
		entity.setObservationDay(observationDay);
		entity.setWeekBucket(dto.weekBucket());
	}

	private void validateBand(PriceObservationDto dto) {
		if (dto.min().signum() < 0 || dto.max().signum() < 0
				|| (dto.typical() != null && dto.typical().signum() < 0)) {
			throw ScrapeException.negativeAmount(dto.scopeKey());
		}
		if (dto.min().compareTo(dto.max()) > 0) {
			throw ScrapeException.minExceedsMax(dto.scopeKey());
		}
		if (dto.typical() != null) {
			if (dto.typical().compareTo(dto.min()) < 0 || dto.typical().compareTo(dto.max()) > 0) {
				throw ScrapeException.typicalOutOfBand(dto.scopeKey());
			}
		}
	}

	private void upsertFuelPrice(PriceObservationDto dto, Instant checkedAt) {
		FuelType fuelType = parseFuelType(dto.scopeKey());
		LocalDate effectiveFrom = checkedAt.atZone(ZoneOffset.UTC).toLocalDate();
		FuelPrice fuel = fuelPriceRepository.findByFuelTypeAndEffectiveFrom(fuelType, effectiveFrom)
				.orElseGet(FuelPrice::new);
		fuel.setFuelType(fuelType);
		fuel.setPricePerLiter(dto.typical() != null ? dto.typical() : dto.min());
		fuel.setEffectiveFrom(effectiveFrom);
		fuel.setSourceUrl(dto.sourceUrl());
		fuel.setCheckedAt(checkedAt);
		fuelPriceRepository.save(fuel);
	}

	private static boolean isFoodDestination(PriceObservationDto dto) {
		return dto.category() == BudgetCategory.FOOD
				&& dto.scopeType() == PriceScopeType.DESTINATION
				&& dto.unit() == PriceUnit.PER_DAY;
	}

	private static String destinationScopeKey(PriceObservationDto dto) {
		if (dto.scopeType() == PriceScopeType.DESTINATION) {
			return CityKeyNormalizer.normalize(dto.scopeKey());
		}
		return dto.scopeKey();
	}

	private void applyFoodBand(PriceObservationDto dto, Instant checkedAt) {
		destinationRepository.findByCityKey(destinationScopeKey(dto)).ifPresent(destination -> {
			destination.setFoodPerPersonPerDayMin(dto.min());
			destination.setFoodPerPersonPerDayMax(dto.max());
			destination.setFoodSourceType(dto.sourceType());
			destination.setFoodCheckedAt(checkedAt);
		});
	}

	private static FuelType parseFuelType(String scopeKey) {
		try {
			return FuelType.valueOf(scopeKey.toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw ScrapeException.invalidFuelScope(scopeKey);
		}
	}
}
