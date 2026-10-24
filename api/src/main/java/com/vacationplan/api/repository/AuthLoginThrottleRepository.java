package com.vacationplan.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vacationplan.api.entity.AuthLoginThrottle;

public interface AuthLoginThrottleRepository extends JpaRepository<AuthLoginThrottle, String> {
}
