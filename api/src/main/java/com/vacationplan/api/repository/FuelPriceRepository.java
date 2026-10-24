package com.vacationplan.api.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vacationplan.api.entity.FuelPrice;
import com.vacationplan.api.model.FuelType;

public interface FuelPriceRepository extends JpaRepository<FuelPrice, Long> {

	Optional<FuelPrice> findFirstByFuelTypeOrderByEffectiveFromDescCheckedAtDesc(FuelType fuelType);

	Optional<FuelPrice> findByFuelTypeAndEffectiveFrom(FuelType fuelType, LocalDate effectiveFrom);
}
