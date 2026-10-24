package com.vacationplan.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vacationplan.api.entity.CatalogActivity;

public interface CatalogActivityRepository extends JpaRepository<CatalogActivity, Long> {

	List<CatalogActivity> findByDestination_CityKey(String cityKey);

	Optional<CatalogActivity> findByDestination_CityKeyAndActivityKey(String cityKey, String activityKey);
}
