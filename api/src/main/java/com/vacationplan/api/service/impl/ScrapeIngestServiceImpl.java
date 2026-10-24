package com.vacationplan.api.service.impl;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vacationplan.api.dto.scraper.ScrapeIngestRequest;
import com.vacationplan.api.dto.scraper.ScrapeIngestResponse;
import com.vacationplan.api.entity.CatalogActivity;
import com.vacationplan.api.entity.Destination;
import com.vacationplan.api.entity.PriceRecord;
import com.vacationplan.api.entity.ScrapeTarget;
import com.vacationplan.api.exception.ScrapeException;
import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.repository.CatalogActivityRepository;
import com.vacationplan.api.repository.DestinationRepository;
import com.vacationplan.api.repository.PriceRecordRepository;
import com.vacationplan.api.repository.ScrapeTargetRepository;
import com.vacationplan.api.service.ScrapeIngestService;

@Service
public class ScrapeIngestServiceImpl implements ScrapeIngestService {

	private final ScrapeTargetRepository scrapeTargetRepository;
	private final DestinationRepository destinationRepository;
	private final CatalogActivityRepository catalogActivityRepository;
	private final PriceRecordRepository priceRecordRepository;
	private final Clock clock;

	public ScrapeIngestServiceImpl(
			ScrapeTargetRepository scrapeTargetRepository,
			DestinationRepository destinationRepository,
			CatalogActivityRepository catalogActivityRepository,
			PriceRecordRepository priceRecordRepository,
			Clock clock
	) {
		this.scrapeTargetRepository = scrapeTargetRepository;
		this.destinationRepository = destinationRepository;
		this.catalogActivityRepository = catalogActivityRepository;
		this.priceRecordRepository = priceRecordRepository;
		this.clock = clock;
	}

	@Override
	@Transactional
	public ScrapeIngestResponse ingest(ScrapeIngestRequest request) {
		ScrapeTarget target = scrapeTargetRepository.findById(request.targetId())
				.orElseThrow(() -> ScrapeException.targetNotFound(request.targetId()));
		if (!target.isEnabled()) {
			throw ScrapeException.targetDisabled(request.targetId());
		}
		if (!target.getCatalogField().acceptsScrape()) {
			throw ScrapeException.lodgingNotViaCatalog(request.targetId());
		}
		if (request.min().compareTo(request.max()) > 0) {
			throw ScrapeException.minExceedsMax();
		}

		Destination destination = destinationRepository.findByCityKey(target.getCityKey())
				.orElseThrow(() -> ScrapeException.destinationNotFound(target.getCityKey()));

		BudgetCategory category = applyToCatalog(destination, target, request);
		Instant checkedAt = request.fetchedAt() != null ? request.fetchedAt() : clock.instant();

		PriceRecord record = new PriceRecord();
		record.setRouteKey(routeKey(target));
		record.setCategory(category);
		record.setLabel(target.getLabel());
		record.setMinAmount(request.min());
		record.setMaxAmount(request.max());
		record.setCurrency(request.currency());
		record.setSourceType(PriceSourceType.SCRAPE);
		record.setSourceUrl(target.getSourceUrl());
		record.setCheckedAt(checkedAt);
		priceRecordRepository.save(record);

		return new ScrapeIngestResponse(target.getId(), target.getCityKey(), true, record.getId());
	}

	private BudgetCategory applyToCatalog(Destination destination, ScrapeTarget target, ScrapeIngestRequest request) {
		return switch (target.getCatalogField()) {
			case BUS_ROUND_TRIP -> {
				destination.setBusRoundTripMin(request.min());
				destination.setBusRoundTripMax(request.max());
				yield BudgetCategory.TRANSPORT;
			}
			case LODGING_PER_NIGHT -> throw ScrapeException.lodgingNotViaCatalog(target.getId());
			case FOOD_PER_PERSON_DAY -> {
				destination.setFoodPerPersonPerDayMin(request.min());
				destination.setFoodPerPersonPerDayMax(request.max());
				yield BudgetCategory.FOOD;
			}
			case LOCAL_TRANSPORT_DAY -> {
				destination.setLocalTransportPerDayMin(request.min());
				destination.setLocalTransportPerDayMax(request.max());
				yield BudgetCategory.LOCAL_TRANSPORT;
			}
			case ACTIVITY_ENTRY -> {
				String activityKey = target.getActivityKey();
				if (activityKey == null || activityKey.isBlank()) {
					throw ScrapeException.activityKeyRequired();
				}
				CatalogActivity activity = catalogActivityRepository
						.findByDestination_CityKeyAndActivityKey(target.getCityKey(), activityKey)
						.orElseThrow(() -> ScrapeException.activityNotFound(activityKey));
				activity.setEntryFeeMin(request.min());
				activity.setEntryFeeMax(request.max());
				activity.setSourceType(PriceSourceType.SCRAPE);
				activity.setSourceUrl(target.getSourceUrl());
				yield BudgetCategory.ACTIVITY;
			}
		};
	}

	private static String routeKey(ScrapeTarget target) {
		if (target.getActivityKey() != null && !target.getActivityKey().isBlank()) {
			return "catalog:" + target.getCityKey() + ":" + target.getCatalogField() + ":" + target.getActivityKey();
		}
		return "catalog:" + target.getCityKey() + ":" + target.getCatalogField();
	}
}
