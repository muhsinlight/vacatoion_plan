package com.vacationplan.api.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.TransportMode;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.model.TripStatus;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "trips")
public class Trip {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id")
	private User owner;

	@Column(name = "origin_city", nullable = false, length = 120)
	private String originCity;

	@Column(name = "destination_city", nullable = false, length = 120)
	private String destinationCity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "destination_id")
	private Destination destination;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(nullable = false)
	private int adults;

	@ElementCollection
	@CollectionTable(name = "trip_child_ages", joinColumns = @JoinColumn(name = "trip_id"))
	@Column(name = "age")
	private List<Integer> childAges = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "transport_mode", length = 20)
	private TransportMode transportMode;

	@Enumerated(EnumType.STRING)
	@Column(name = "accommodation_type", length = 20)
	private AccommodationType accommodationType;

	@Enumerated(EnumType.STRING)
	@Column(name = "travel_style", length = 20)
	private TravelStyle travelStyle;

	@ElementCollection
	@CollectionTable(name = "trip_interests", joinColumns = @JoinColumn(name = "trip_id"))
	@Column(name = "interest", length = 80)
	private List<String> interests = new ArrayList<>();

	@Column(name = "total_budget", precision = 12, scale = 2)
	private BigDecimal totalBudget;

	@Column(name = "per_person_budget", precision = 12, scale = 2)
	private BigDecimal perPersonBudget;

	@Column(name = "include_contingency", nullable = false)
	private boolean includeContingency = true;

	@ElementCollection
	@CollectionTable(name = "trip_selected_activities", joinColumns = @JoinColumn(name = "trip_id"))
	@Column(name = "activity_key", length = 80)
	private List<String> selectedActivityKeys = new ArrayList<>();

	@Column(name = "special_needs", length = 1000)
	private String specialNeeds;

	@ElementCollection
	@CollectionTable(name = "trip_custom_activities", joinColumns = @JoinColumn(name = "trip_id"))
	@Column(name = "activity_name", length = 120)
	private List<String> customActivityNames = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TripStatus status = TripStatus.DRAFT;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public void markUpdated(Instant instant) {
		updatedAt = instant;
	}

	public Long getId() {
		return id;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public String getOriginCity() {
		return originCity;
	}

	public void setOriginCity(String originCity) {
		this.originCity = originCity;
	}

	public String getDestinationCity() {
		return destinationCity;
	}

	public void setDestinationCity(String destinationCity) {
		this.destinationCity = destinationCity;
	}

	public Destination getDestination() {
		return destination;
	}

	public void setDestination(Destination destination) {
		this.destination = destination;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public int getAdults() {
		return adults;
	}

	public void setAdults(int adults) {
		this.adults = adults;
	}

	public List<Integer> getChildAges() {
		return childAges;
	}

	public void setChildAges(List<Integer> childAges) {
		this.childAges = new ArrayList<>(childAges);
	}

	public TransportMode getTransportMode() {
		return transportMode;
	}

	public void setTransportMode(TransportMode transportMode) {
		this.transportMode = transportMode;
	}

	public AccommodationType getAccommodationType() {
		return accommodationType;
	}

	public void setAccommodationType(AccommodationType accommodationType) {
		this.accommodationType = accommodationType;
	}

	public TravelStyle getTravelStyle() {
		return travelStyle;
	}

	public void setTravelStyle(TravelStyle travelStyle) {
		this.travelStyle = travelStyle;
	}

	public List<String> getInterests() {
		return interests;
	}

	public void setInterests(List<String> interests) {
		this.interests = new ArrayList<>(interests);
	}

	public BigDecimal getTotalBudget() {
		return totalBudget;
	}

	public void setTotalBudget(BigDecimal totalBudget) {
		this.totalBudget = totalBudget;
	}

	public BigDecimal getPerPersonBudget() {
		return perPersonBudget;
	}

	public void setPerPersonBudget(BigDecimal perPersonBudget) {
		this.perPersonBudget = perPersonBudget;
	}

	public boolean isIncludeContingency() {
		return includeContingency;
	}

	public void setIncludeContingency(boolean includeContingency) {
		this.includeContingency = includeContingency;
	}

	public List<String> getSelectedActivityKeys() {
		return selectedActivityKeys;
	}

	public void setSelectedActivityKeys(List<String> selectedActivityKeys) {
		this.selectedActivityKeys = selectedActivityKeys == null ? new ArrayList<>() : new ArrayList<>(selectedActivityKeys);
	}

	public String getSpecialNeeds() {
		return specialNeeds;
	}

	public void setSpecialNeeds(String specialNeeds) {
		this.specialNeeds = specialNeeds;
	}

	public List<String> getCustomActivityNames() {
		return customActivityNames;
	}

	public void setCustomActivityNames(List<String> customActivityNames) {
		this.customActivityNames = customActivityNames == null ? new ArrayList<>() : new ArrayList<>(customActivityNames);
	}

	public TripStatus getStatus() {
		return status;
	}

	public void setStatus(TripStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
