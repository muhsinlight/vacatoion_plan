package com.vacationplan.api.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tek cihaz çıkışında access token'ın {@code jti} değeri, kendi süresine kadar burada durur.
 */
@Entity
@Table(name = "revoked_access_tokens")
public class RevokedAccessToken {

	@Id
	@Column(nullable = false, length = 36)
	private String jti;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	public String getJti() {
		return jti;
	}

	public void setJti(String jti) {
		this.jti = jti;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}
}
