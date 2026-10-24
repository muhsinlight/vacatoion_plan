package com.vacationplan.api.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vacationplan.api.entity.PriceObservation;
import com.vacationplan.api.model.PriceScopeType;
import com.vacationplan.api.model.PriceUnit;

public interface PriceObservationRepository extends JpaRepository<PriceObservation, Long> {

	Optional<PriceObservation> findByScopeTypeAndScopeKeyAndUnitAndObservationDayAndWeekBucket(
			PriceScopeType scopeType,
			String scopeKey,
			PriceUnit unit,
			LocalDate observationDay,
			LocalDate weekBucket
	);

	Optional<PriceObservation> findByScopeTypeAndScopeKeyAndUnitAndObservationDayAndWeekBucketIsNull(
			PriceScopeType scopeType,
			String scopeKey,
			PriceUnit unit,
			LocalDate observationDay
	);

	void deleteByCheckedAtBefore(Instant cutoff);
}
