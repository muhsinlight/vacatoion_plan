package com.vacationplan.api.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vacationplan.api.entity.AuthRefreshReplay;

public interface AuthRefreshReplayRepository extends JpaRepository<AuthRefreshReplay, String> {

	Optional<AuthRefreshReplay> findByPreviousTokenHash(String previousTokenHash);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from AuthRefreshReplay replay where replay.user.id = :userId")
	void deleteByUserId(@Param("userId") Long userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from AuthRefreshReplay replay where replay.expiresAt < :cutoff")
	void deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
