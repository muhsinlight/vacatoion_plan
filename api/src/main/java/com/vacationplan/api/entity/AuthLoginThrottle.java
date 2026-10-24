package com.vacationplan.api.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_login_throttle")
public class AuthLoginThrottle {

	@Id
	@Column(nullable = false, length = 320)
	private String email;

	@Column(name = "window_start", nullable = false)
	private Instant windowStart;

	@Column(name = "failure_count", nullable = false)
	private int failureCount;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Instant getWindowStart() {
		return windowStart;
	}

	public void setWindowStart(Instant windowStart) {
		this.windowStart = windowStart;
	}

	public int getFailureCount() {
		return failureCount;
	}

	public void setFailureCount(int failureCount) {
		this.failureCount = failureCount;
	}
}
