package com.vacationplan.api.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vacationplan.api.entity.RevokedAccessToken;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from RevokedAccessToken t where t.expiresAt < :now")
	int deleteExpired(@Param("now") Instant now);
}
