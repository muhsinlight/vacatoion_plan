package com.vacationplan.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.vacationplan.api.entity.Trip;

public interface TripRepository extends JpaRepository<Trip, Long> {

	Page<Trip> findByOwnerIdOrderByIdAsc(Long ownerId, Pageable pageable);

	Page<Trip> findAllByOrderByIdAsc(Pageable pageable);
}
