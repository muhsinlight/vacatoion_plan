package com.vacationplan.api.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.PriceScopeType;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.model.PriceUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "price_observations")
public class PriceObservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private BudgetCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "scope_type", nullable = false, length = 40)
	private PriceScopeType scopeType;

	@Column(name = "scope_key", nullable = false, length = 120)
	private String scopeKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PriceUnit unit;

	@Column(name = "min_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal minAmount;

	@Column(name = "typical_amount", precision = 12, scale = 2)
	private BigDecimal typicalAmount;

	@Column(name = "max_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal maxAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 30)
	private PriceSourceType sourceType;

	@Column(length = 80)
	private String provider;

	@Column(name = "source_url", length = 500)
	private String sourceUrl;

	@Column(name = "sample_size")
	private Integer sampleSize;

	@Column(precision = 3, scale = 2)
	private BigDecimal confidence;

	@Column(name = "checked_at", nullable = false)
	private Instant checkedAt;

	@Column(name = "observation_day", nullable = false)
	private LocalDate observationDay;

	@Column(name = "week_bucket")
	private LocalDate weekBucket;

	public Long getId() {
		return id;
	}

	public BudgetCategory getCategory() {
		return category;
	}

	public void setCategory(BudgetCategory category) {
		this.category = category;
	}

	public PriceScopeType getScopeType() {
		return scopeType;
	}

	public void setScopeType(PriceScopeType scopeType) {
		this.scopeType = scopeType;
	}

	public String getScopeKey() {
		return scopeKey;
	}

	public void setScopeKey(String scopeKey) {
		this.scopeKey = scopeKey;
	}

	public PriceUnit getUnit() {
		return unit;
	}

	public void setUnit(PriceUnit unit) {
		this.unit = unit;
	}

	public BigDecimal getMinAmount() {
		return minAmount;
	}

	public void setMinAmount(BigDecimal minAmount) {
		this.minAmount = minAmount;
	}

	public BigDecimal getTypicalAmount() {
		return typicalAmount;
	}

	public void setTypicalAmount(BigDecimal typicalAmount) {
		this.typicalAmount = typicalAmount;
	}

	public BigDecimal getMaxAmount() {
		return maxAmount;
	}

	public void setMaxAmount(BigDecimal maxAmount) {
		this.maxAmount = maxAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public PriceSourceType getSourceType() {
		return sourceType;
	}

	public void setSourceType(PriceSourceType sourceType) {
		this.sourceType = sourceType;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public Integer getSampleSize() {
		return sampleSize;
	}

	public void setSampleSize(Integer sampleSize) {
		this.sampleSize = sampleSize;
	}

	public BigDecimal getConfidence() {
		return confidence;
	}

	public void setConfidence(BigDecimal confidence) {
		this.confidence = confidence;
	}

	public Instant getCheckedAt() {
		return checkedAt;
	}

	public void setCheckedAt(Instant checkedAt) {
		this.checkedAt = checkedAt;
	}

	public LocalDate getObservationDay() {
		return observationDay;
	}

	public void setObservationDay(LocalDate observationDay) {
		this.observationDay = observationDay;
	}

	public LocalDate getWeekBucket() {
		return weekBucket;
	}

	public void setWeekBucket(LocalDate weekBucket) {
		this.weekBucket = weekBucket;
	}
}
