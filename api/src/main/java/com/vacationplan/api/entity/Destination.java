package com.vacationplan.api.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.vacationplan.api.model.PriceSourceType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "destinations",
		uniqueConstraints = @UniqueConstraint(name = "uk_destinations_city_key", columnNames = "city_key")
)
public class Destination {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "city_key", nullable = false, length = 80)
	private String cityKey;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	@Column(name = "bus_min", nullable = false, precision = 12, scale = 2)
	private BigDecimal busRoundTripMin;

	@Column(name = "bus_max", nullable = false, precision = 12, scale = 2)
	private BigDecimal busRoundTripMax;

	@Column(name = "train_min", precision = 12, scale = 2)
	private BigDecimal trainRoundTripMin;

	@Column(name = "train_max", precision = 12, scale = 2)
	private BigDecimal trainRoundTripMax;

	@Column(name = "plane_min", precision = 12, scale = 2)
	private BigDecimal planeRoundTripMin;

	@Column(name = "plane_max", precision = 12, scale = 2)
	private BigDecimal planeRoundTripMax;

	@Column(name = "car_min", precision = 12, scale = 2)
	private BigDecimal carRoundTripMin;

	@Column(name = "car_max", precision = 12, scale = 2)
	private BigDecimal carRoundTripMax;

	@Column(name = "lodging_min", nullable = false, precision = 12, scale = 2)
	private BigDecimal lodgingPerNightMin;

	@Column(name = "lodging_max", nullable = false, precision = 12, scale = 2)
	private BigDecimal lodgingPerNightMax;

	@Column(name = "food_min", nullable = false, precision = 12, scale = 2)
	private BigDecimal foodPerPersonPerDayMin;

	@Column(name = "food_max", nullable = false, precision = 12, scale = 2)
	private BigDecimal foodPerPersonPerDayMax;

	@Enumerated(EnumType.STRING)
	@Column(name = "food_source_type", length = 30)
	private PriceSourceType foodSourceType;

	@Column(name = "food_checked_at")
	private Instant foodCheckedAt;

	@Column(name = "food_places", columnDefinition = "text")
	private String foodPlacesJson;

	@Column(name = "local_transport_min", nullable = false, precision = 12, scale = 2)
	private BigDecimal localTransportPerDayMin;

	@Column(name = "local_transport_max", nullable = false, precision = 12, scale = 2)
	private BigDecimal localTransportPerDayMax;

	@OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<CatalogActivity> activities = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public String getCityKey() {
		return cityKey;
	}

	public void setCityKey(String cityKey) {
		this.cityKey = cityKey;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public BigDecimal getBusRoundTripMin() {
		return busRoundTripMin;
	}

	public void setBusRoundTripMin(BigDecimal busRoundTripMin) {
		this.busRoundTripMin = busRoundTripMin;
	}

	public BigDecimal getBusRoundTripMax() {
		return busRoundTripMax;
	}

	public void setBusRoundTripMax(BigDecimal busRoundTripMax) {
		this.busRoundTripMax = busRoundTripMax;
	}

	public BigDecimal getTrainRoundTripMin() {
		return trainRoundTripMin;
	}

	public void setTrainRoundTripMin(BigDecimal trainRoundTripMin) {
		this.trainRoundTripMin = trainRoundTripMin;
	}

	public BigDecimal getTrainRoundTripMax() {
		return trainRoundTripMax;
	}

	public void setTrainRoundTripMax(BigDecimal trainRoundTripMax) {
		this.trainRoundTripMax = trainRoundTripMax;
	}

	public BigDecimal getPlaneRoundTripMin() {
		return planeRoundTripMin;
	}

	public void setPlaneRoundTripMin(BigDecimal planeRoundTripMin) {
		this.planeRoundTripMin = planeRoundTripMin;
	}

	public BigDecimal getPlaneRoundTripMax() {
		return planeRoundTripMax;
	}

	public void setPlaneRoundTripMax(BigDecimal planeRoundTripMax) {
		this.planeRoundTripMax = planeRoundTripMax;
	}

	public BigDecimal getCarRoundTripMin() {
		return carRoundTripMin;
	}

	public void setCarRoundTripMin(BigDecimal carRoundTripMin) {
		this.carRoundTripMin = carRoundTripMin;
	}

	public BigDecimal getCarRoundTripMax() {
		return carRoundTripMax;
	}

	public void setCarRoundTripMax(BigDecimal carRoundTripMax) {
		this.carRoundTripMax = carRoundTripMax;
	}

	public BigDecimal getLodgingPerNightMin() {
		return lodgingPerNightMin;
	}

	public void setLodgingPerNightMin(BigDecimal lodgingPerNightMin) {
		this.lodgingPerNightMin = lodgingPerNightMin;
	}

	public BigDecimal getLodgingPerNightMax() {
		return lodgingPerNightMax;
	}

	public void setLodgingPerNightMax(BigDecimal lodgingPerNightMax) {
		this.lodgingPerNightMax = lodgingPerNightMax;
	}

	public BigDecimal getFoodPerPersonPerDayMin() {
		return foodPerPersonPerDayMin;
	}

	public void setFoodPerPersonPerDayMin(BigDecimal foodPerPersonPerDayMin) {
		this.foodPerPersonPerDayMin = foodPerPersonPerDayMin;
	}

	public BigDecimal getFoodPerPersonPerDayMax() {
		return foodPerPersonPerDayMax;
	}

	public void setFoodPerPersonPerDayMax(BigDecimal foodPerPersonPerDayMax) {
		this.foodPerPersonPerDayMax = foodPerPersonPerDayMax;
	}

	public PriceSourceType getFoodSourceType() {
		return foodSourceType;
	}

	public void setFoodSourceType(PriceSourceType foodSourceType) {
		this.foodSourceType = foodSourceType;
	}

	public Instant getFoodCheckedAt() {
		return foodCheckedAt;
	}

	public void setFoodCheckedAt(Instant foodCheckedAt) {
		this.foodCheckedAt = foodCheckedAt;
	}

	public String getFoodPlacesJson() {
		return foodPlacesJson;
	}

	public void setFoodPlacesJson(String foodPlacesJson) {
		this.foodPlacesJson = foodPlacesJson;
	}

	public BigDecimal getLocalTransportPerDayMin() {
		return localTransportPerDayMin;
	}

	public void setLocalTransportPerDayMin(BigDecimal localTransportPerDayMin) {
		this.localTransportPerDayMin = localTransportPerDayMin;
	}

	public BigDecimal getLocalTransportPerDayMax() {
		return localTransportPerDayMax;
	}

	public void setLocalTransportPerDayMax(BigDecimal localTransportPerDayMax) {
		this.localTransportPerDayMax = localTransportPerDayMax;
	}

	public List<CatalogActivity> getActivities() {
		return activities;
	}

	public void addActivity(CatalogActivity activity) {
		activities.add(activity);
		activity.setDestination(this);
	}
}
