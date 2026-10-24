package com.vacationplan.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vacationplan.api.entity.ScrapeTarget;

public interface ScrapeTargetRepository extends JpaRepository<ScrapeTarget, Long> {

	List<ScrapeTarget> findByEnabledTrueOrderByCityKeyAscIdAsc();
}
