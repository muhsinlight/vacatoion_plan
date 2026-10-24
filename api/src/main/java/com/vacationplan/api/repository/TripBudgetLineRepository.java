package com.vacationplan.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vacationplan.api.entity.TripBudgetLine;

public interface TripBudgetLineRepository extends JpaRepository<TripBudgetLine, Long> {

	List<TripBudgetLine> findByTripIdOrderByIdAsc(Long tripId);

	List<TripBudgetLine> findByTrip_IdInOrderByIdAsc(List<Long> tripIds);

	void deleteByTripId(Long tripId);
}
