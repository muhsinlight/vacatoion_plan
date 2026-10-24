package com.vacationplan.api.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vacationplan.api.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	@Query("select t from RefreshToken t join fetch t.user where t.tokenHash = :hash")
	Optional<RefreshToken> findByTokenHash(@Param("hash") String hash);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update RefreshToken t set t.revokedAt = :at where t.user.id = :userId and t.revokedAt is null")
	int revokeActiveByUserId(@Param("userId") Long userId, @Param("at") Instant at);

	/**
	 * Aynı refresh ile gelen paralel isteklerden yalnız biri satırı iptal eder.
	 *
	 * @return iptal edilen satır sayısı; 0 ise başka istek önce davrandı
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update RefreshToken t set t.revokedAt = :at where t.tokenHash = :hash and t.revokedAt is null")
	int revokeIfActive(@Param("hash") String hash, @Param("at") Instant at);
}
