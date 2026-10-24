package com.vacationplan.api.entity;

import java.math.BigDecimal;

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
 * Kayıtlı bir seyahate bağlı bütçe satırı (preview sonrası kalıcı döküm).
 */
@Entity
@Table(name = "trip_budget_lines")
public class TripBudgetLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trip_id", nullable = false)
	private Trip trip;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private BudgetCategory category;

	@Column(nullable = false, length = 200)
	private String label;

	@Column(name = "min_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal minAmount;

	@Column(name = "max_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal maxAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 30)
	private PriceSourceType sourceType;

	@Column(name = "included_in_total", nullable = false)
	private boolean includedInTotal;

	public Long getId() {
		return id;
	}

	public Trip getTrip() {
		return trip;
	}

	public void setTrip(Trip trip) {
		this.trip = trip;
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

	public boolean isIncludedInTotal() {
		return includedInTotal;
	}

	public void setIncludedInTotal(boolean includedInTotal) {
		this.includedInTotal = includedInTotal;
	}
}
