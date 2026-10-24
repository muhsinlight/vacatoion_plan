package com.vacationplan.api.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.vacationplan.api.model.FuelType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fuel_prices")
public class FuelPrice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "fuel_type", nullable = false, length = 20)
	private FuelType fuelType;

	@Column(name = "price_per_liter", nullable = false, precision = 12, scale = 2)
	private BigDecimal pricePerLiter;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "source_url", length = 500)
	private String sourceUrl;

	@Column(name = "checked_at", nullable = false)
	private Instant checkedAt;

	public Long getId() {
		return id;
	}

	public FuelType getFuelType() {
		return fuelType;
	}

	public void setFuelType(FuelType fuelType) {
		this.fuelType = fuelType;
	}

	public BigDecimal getPricePerLiter() {
		return pricePerLiter;
	}

	public void setPricePerLiter(BigDecimal pricePerLiter) {
		this.pricePerLiter = pricePerLiter;
	}

	public LocalDate getEffectiveFrom() {
		return effectiveFrom;
	}

	public void setEffectiveFrom(LocalDate effectiveFrom) {
		this.effectiveFrom = effectiveFrom;
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
