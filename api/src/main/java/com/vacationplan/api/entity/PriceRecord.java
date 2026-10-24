package com.vacationplan.api.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.PriceSourceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Veritabanında tek satır = tek bütçe kalemi anlık görüntüsü (ör. "Gidiş-dönüş ulaşım", min/max TRY).
 * Tüm tatili tek tabloda tutmaz; ileride Trip, Activity vb. ayrı entity'ler eklenir.
 * Şimdilik sadece isteğe bağlı arşiv (vacation.save-price-snapshots=true).
 */
@Entity
@Table(name = "price_records")
public class PriceRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trip_id")
	private Trip trip;

	@Column(nullable = false, length = 120)
	private String routeKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private BudgetCategory category;

	@Column(nullable = false, length = 200)
	private String label;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal minAmount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal maxAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PriceSourceType sourceType;

	@Column(length = 500)
	private String sourceUrl;

	@Column(nullable = false)
	private Instant checkedAt;

	public Long getId() {
		return id;
	}

	public Trip getTrip() {
		return trip;
	}

	public void setTrip(Trip trip) {
		this.trip = trip;
	}

	public String getRouteKey() {
		return routeKey;
	}

	public void setRouteKey(String routeKey) {
		this.routeKey = routeKey;
	}

	public BudgetCategory getCategory() {
		return category;
	}

	public void setCategory(BudgetCategory category) {
		this.category = category;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public BigDecimal getMinAmount() {
		return minAmount;
	}

	public void setMinAmount(BigDecimal minAmount) {
		this.minAmount = minAmount;
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

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public Instant getCheckedAt() {
		return checkedAt;
	}

	public void setCheckedAt(Instant checkedAt) {
		this.checkedAt = checkedAt;
	}
}
