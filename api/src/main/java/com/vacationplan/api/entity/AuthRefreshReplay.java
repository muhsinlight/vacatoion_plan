package com.vacationplan.api.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_refresh_replay")
public class AuthRefreshReplay {

	@Id
	@Column(name = "previous_token_hash", nullable = false, length = 64)
	private String previousTokenHash;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	/** SHA-256 hex; düz metin JWT yazılmaz. */
	@Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
	private String accessToken;

	/** SHA-256 hex; düz metin yenileme yazılmaz. */
	@Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
	private String refreshToken;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	public String getPreviousTokenHash() {
		return previousTokenHash;
	}

	public void setPreviousTokenHash(String previousTokenHash) {
		this.previousTokenHash = previousTokenHash;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}
}
