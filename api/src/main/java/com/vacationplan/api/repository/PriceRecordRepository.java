package com.vacationplan.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vacationplan.api.entity.PriceRecord;

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {
}
