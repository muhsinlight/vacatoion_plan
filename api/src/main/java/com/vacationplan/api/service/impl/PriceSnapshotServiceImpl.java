package com.vacationplan.api.service.impl;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.entity.PriceRecord;
import com.vacationplan.api.entity.Trip;
import com.vacationplan.api.model.BudgetLineItem;
import com.vacationplan.api.repository.PriceRecordRepository;
import com.vacationplan.api.service.PriceSnapshotService;
import com.vacationplan.api.service.model.TripPlanResult;

@Service
public class PriceSnapshotServiceImpl implements PriceSnapshotService {

	private final PriceRecordRepository priceRecordRepository;
	private final VacationProperties vacationProperties;
	private final Clock clock;

	public PriceSnapshotServiceImpl(
			PriceRecordRepository priceRecordRepository,
			VacationProperties vacationProperties,
			Clock clock
	) {
		this.priceRecordRepository = priceRecordRepository;
		this.vacationProperties = vacationProperties;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void saveForTrip(Trip trip, TripPlanResult plan) {
		if (!vacationProperties.isSavePriceSnapshots()) {
			return;
		}
		String routeKey = trip.getOriginCity().trim().toLowerCase() + "->" + trip.getDestinationCity().trim().toLowerCase();
		for (BudgetLineItem line : plan.budget().lineItems()) {
			if (!line.selected()) {
				continue;
			}
			PriceRecord record = new PriceRecord();
			record.setTrip(trip);
			record.setRouteKey(routeKey);
			record.setCategory(line.category());
			record.setLabel(line.label());
			record.setMinAmount(line.amount().min());
			record.setMaxAmount(line.amount().max());
			record.setCurrency(line.amount().currency());
			record.setSourceType(line.sourceType());
			record.setCheckedAt(clock.instant());
			priceRecordRepository.save(record);
		}
	}
}
