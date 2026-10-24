package com.vacationplan.api.entity;

import java.time.Instant;

import com.vacationplan.api.model.QuotaBucket;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "request_quota_windows",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_request_quota_bucket_scope_window",
				columnNames = { "bucket", "scope_key", "window_start" }
		)
)
public class RequestQuotaWindow {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private QuotaBucket bucket;

	@Column(name = "scope_key", nullable = false, length = 80)
	private String scopeKey = "shared";

	@Column(name = "window_start", nullable = false)
	private Instant windowStart;

	@Column(name = "used_count", nullable = false)
	private int usedCount;

	public Long getId() {
		return id;
	}

	public QuotaBucket getBucket() {
		return bucket;
	}

	public void setBucket(QuotaBucket bucket) {
		this.bucket = bucket;
	}

	public String getScopeKey() {
		return scopeKey;
	}

	public void setScopeKey(String scopeKey) {
		this.scopeKey = scopeKey;
	}

	public Instant getWindowStart() {
		return windowStart;
	}

	public void setWindowStart(Instant windowStart) {
		this.windowStart = windowStart;
	}

	public int getUsedCount() {
		return usedCount;
	}

	public void setUsedCount(int usedCount) {
		this.usedCount = usedCount;
	}
}
