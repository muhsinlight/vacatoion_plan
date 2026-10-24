package com.vacationplan.api.entity;

import java.math.BigDecimal;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "catalog_activities",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_catalog_activities_dest_key",
				columnNames = { "destination_id", "activity_key" }
		)
)
public class CatalogActivity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "destination_id", nullable = false)
	private Destination destination;

	@Column(name = "activity_key", nullable = false, length = 80)
	private String activityKey;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, length = 500)
	private String description;

	@Column(name = "estimated_minutes", nullable = false)
	private int estimatedMinutes;

	@Column(name = "entry_fee_min", nullable = false, precision = 12, scale = 2)
	private BigDecimal entryFeeMin;

	@Column(name = "entry_fee_max", nullable = false, precision = 12, scale = 2)
	private BigDecimal entryFeeMax;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 30)
	private PriceSourceType sourceType;

	@Column(name = "source_url", length = 500)
	private String sourceUrl;

	@Column(name = "default_selected", nullable = false)
	private boolean defaultSelected;

	public Long getId() {
		return id;
	}

	public String getActivityKey() {
		return activityKey;
	}

	public void setActivityKey(String activityKey) {
		this.activityKey = activityKey;
	}

	public Destination getDestination() {
		return destination;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getEstimatedMinutes() {
		return estimatedMinutes;
	}

	public void setEstimatedMinutes(int estimatedMinutes) {
		this.estimatedMinutes = estimatedMinutes;
	}

	public BigDecimal getEntryFeeMin() {
		return entryFeeMin;
	}

	public void setEntryFeeMin(BigDecimal entryFeeMin) {
		this.entryFeeMin = entryFeeMin;
	}

	public BigDecimal getEntryFeeMax() {
		return entryFeeMax;
	}

	public void setEntryFeeMax(BigDecimal entryFeeMax) {
		this.entryFeeMax = entryFeeMax;
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

	public boolean isDefaultSelected() {
		return defaultSelected;
	}

	public void setDefaultSelected(boolean defaultSelected) {
		this.defaultSelected = defaultSelected;
	}
}
