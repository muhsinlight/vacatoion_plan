package com.vacationplan.api.security;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vacationplan.api.entity.RevokedAccessToken;
import com.vacationplan.api.repository.RevokedAccessTokenRepository;

/**
 * Tek cihaz çıkışının access token'ı, süresi dolmadan da reddedilir.
 */
@Component
public class AccessTokenRevocation {

	private final RevokedAccessTokenRepository repository;
	private final Clock clock;

	public AccessTokenRevocation(RevokedAccessTokenRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	public boolean isRevoked(String jti) {
		return jti != null && repository.existsById(jti);
	}

	@Transactional
	public void revokeUntil(String jti, Instant expiresAt) {
		if (jti == null || jti.isBlank() || expiresAt == null || !expiresAt.isAfter(clock.instant())) {
			return;
		}
		repository.deleteExpired(clock.instant());
		if (repository.existsById(jti)) {
			return;
		}
		RevokedAccessToken revoked = new RevokedAccessToken();
		revoked.setJti(jti);
		revoked.setExpiresAt(expiresAt);
		repository.save(revoked);
	}
}
