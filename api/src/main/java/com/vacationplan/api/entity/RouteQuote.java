package com.vacationplan.api.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.vacationplan.api.model.TransportMode;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "route_quotes",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_route_quotes_route_week",
				columnNames = { "origin_key", "dest_key", "transport_mode", "week_bucket" }
		)
)
public class RouteQuote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "origin_key", nullable = false, length = 80)
	private String originKey;

	@Column(name = "dest_key", nullable = false, length = 80)
	private String destKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "transport_mode", nullable = false, length = 20)
	private TransportMode transportMode;

	@Column(name = "week_bucket", nullable = false)
	private LocalDate weekBucket;

	@Column(name = "min_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal minAmount;

	@Column(name = "typical_amount", precision = 12, scale = 2)
	private BigDecimal typicalAmount;

	@Column(name = "max_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal maxAmount;

	@Column(nullable = false, length = 3)
	private String currency;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "observation_id")
	private PriceObservation observation;

	@Column(name = "checked_at", nullable = false)
	private Instant checkedAt;

	public Long getId() {
		return id;
	}

	public String getOriginKey() {
		return originKey;
	}

	public void setOriginKey(String originKey) {
		this.originKey = originKey;
	}

	public String getDestKey() {
		return destKey;
	}

	public void setDestKey(String destKey) {
		this.destKey = destKey;
	}

	public TransportMode getTransportMode() {
		return transportMode;
	}

	public void setTransportMode(TransportMode transportMode) {
		this.transportMode = transportMode;
	}

	public LocalDate getWeekBucket() {
		return weekBucket;
	}

	public void setWeekBucket(LocalDate weekBucket) {
		this.weekBucket = weekBucket;
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

	public PriceObservation getObservation() {
		return observation;
	}

	public void setObservation(PriceObservation observation) {
		this.observation = observation;
	}

	public Instant getCheckedAt() {
		return checkedAt;
	}

	public void setCheckedAt(Instant checkedAt) {
		this.checkedAt = checkedAt;
	}
}
