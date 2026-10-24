package com.vacationplan.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vacationplan.api.entity.Destination;

public interface DestinationRepository extends JpaRepository<Destination, Long> {

	Optional<Destination> findByCityKey(String cityKey);
}
