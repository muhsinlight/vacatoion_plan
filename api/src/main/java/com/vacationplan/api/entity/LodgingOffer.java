package com.vacationplan.api.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.LodgingTier;

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
		name = "lodging_offers",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_lodging_offers_lookup_tier",
				columnNames = { "city_key", "accommodation_type", "check_in", "check_out", "tier" }
		)
)
public class LodgingOffer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "city_key", nullable = false, length = 80)
	private String cityKey;

	@Column(nullable = false, length = 40)
	private String provider;

	@Enumerated(EnumType.STRING)
	@Column(name = "accommodation_type", nullable = false, length = 20)
	private AccommodationType accommodationType;

	@Column(name = "check_in", nullable = false)
	private LocalDate checkIn;

	@Column(name = "check_out", nullable = false)
	private LocalDate checkOut;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LodgingTier tier;

	@Column(nullable = false, length = 200)
	private String name;

	private Integer stars;

	@Column(precision = 4, scale = 2)
	private BigDecimal rating;

	@Column(name = "board_type", length = 80)
	private String boardType;

	@Column(length = 500)
	private String features;

	@Column(name = "price_per_night", nullable = false, precision = 12, scale = 2)
	private BigDecimal pricePerNight;

	@Column(length = 500)
	private String url;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "band_min", precision = 12, scale = 2)
	private BigDecimal bandMin;

	@Column(name = "band_typical", precision = 12, scale = 2)
	private BigDecimal bandTypical;

	@Column(name = "band_max", precision = 12, scale = 2)
	private BigDecimal bandMax;

	@Column(name = "checked_at", nullable = false)
	private Instant checkedAt;

	@Column(nullable = false)
	private boolean estimated;

	public Long getId() {
		return id;
	}

	public String getCityKey() {
		return cityKey;
	}

	public void setCityKey(String cityKey) {
		this.cityKey = cityKey;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public AccommodationType getAccommodationType() {
		return accommodationType;
	}

	public void setAccommodationType(AccommodationType accommodationType) {
		this.accommodationType = accommodationType;
	}

	public LocalDate getCheckIn() {
		return checkIn;
	}

	public void setCheckIn(LocalDate checkIn) {
		this.checkIn = checkIn;
	}

	public LocalDate getCheckOut() {
		return checkOut;
	}

	public void setCheckOut(LocalDate checkOut) {
		this.checkOut = checkOut;
	}

	public LodgingTier getTier() {
		return tier;
	}

	public void setTier(LodgingTier tier) {
		this.tier = tier;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getStars() {
		return stars;
	}

	public void setStars(Integer stars) {
		this.stars = stars;
	}

	public BigDecimal getRating() {
		return rating;
	}

	public void setRating(BigDecimal rating) {
		this.rating = rating;
	}

	public String getBoardType() {
		return boardType;
	}

	public void setBoardType(String boardType) {
		this.boardType = boardType;
	}

	public String getFeatures() {
		return features;
	}

	public void setFeatures(String features) {
		this.features = features;
	}

	public BigDecimal getPricePerNight() {
		return pricePerNight;
	}

	public void setPricePerNight(BigDecimal pricePerNight) {
		this.pricePerNight = pricePerNight;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public BigDecimal getBandMin() {
		return bandMin;
	}

	public void setBandMin(BigDecimal bandMin) {
		this.bandMin = bandMin;
	}

	public BigDecimal getBandTypical() {
		return bandTypical;
	}

	public void setBandTypical(BigDecimal bandTypical) {
		this.bandTypical = bandTypical;
	}

	public BigDecimal getBandMax() {
		return bandMax;
	}

	public void setBandMax(BigDecimal bandMax) {
		this.bandMax = bandMax;
	}

	public boolean isEstimated() {
		return estimated;
	}

	public void setEstimated(boolean estimated) {
		this.estimated = estimated;
	}

	public Instant getCheckedAt() {
		return checkedAt;
	}

	public void setCheckedAt(Instant checkedAt) {
		this.checkedAt = checkedAt;
	}
}
