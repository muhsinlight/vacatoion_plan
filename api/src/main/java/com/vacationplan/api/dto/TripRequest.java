package com.vacationplan.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.vacationplan.api.model.AccommodationType;
import com.vacationplan.api.model.TransportMode;
import com.vacationplan.api.model.TravelStyle;
import com.vacationplan.api.validation.EndDateNotBeforeStart;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@EndDateNotBeforeStart
public record TripRequest(
		@NotBlank @Size(max = 120) String originCity,
		@NotBlank @Size(max = 120) String destinationCity,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate,
		@NotNull @Min(1) @Max(20) Integer adults,
		@Size(max = 8) List<@NotNull @Min(value = 0, message = "Çocuk yaşı 0 ile 17 arasında olmalı") @Max(value = 17, message = "Çocuk yaşı 0 ile 17 arasında olmalı") Integer> childAges,
		TransportMode transportMode,
		AccommodationType accommodationType,
		TravelStyle travelStyle,
		@Size(max = 32) List<@Size(max = 80) String> interests,
		@DecimalMin("0") BigDecimal totalBudget,
		@DecimalMin("0") BigDecimal perPersonBudget,
		Boolean includeContingency,
		@Size(max = 40) List<@Size(max = 80) String> selectedActivityIds,
		@Size(max = 1000) String specialNeeds,
		@Size(max = 20) List<@Size(max = 120) String> customActivityNames
) {
}
