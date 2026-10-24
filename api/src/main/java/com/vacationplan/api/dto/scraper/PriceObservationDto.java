package com.vacationplan.api.dto.scraper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import com.vacationplan.api.model.BudgetCategory;
import com.vacationplan.api.model.PriceScopeType;
import com.vacationplan.api.model.PriceSourceType;
import com.vacationplan.api.model.PriceUnit;
import com.vacationplan.api.model.TransportMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PriceObservationDto(
		@NotNull BudgetCategory category,
		@NotNull PriceScopeType scopeType,
		@NotBlank @Size(max = 200) String scopeKey,
		@NotNull PriceUnit unit,
		@NotNull @DecimalMin("0") BigDecimal min,
		@DecimalMin("0") BigDecimal typical,
		@NotNull @DecimalMin("0") BigDecimal max,
		@Pattern(regexp = "^[A-Z]{3}$") String currency,
		@NotNull PriceSourceType sourceType,
		@Size(max = 120) String provider,
		@Size(max = 2048) String sourceUrl,
		Integer sampleSize,
		BigDecimal confidence,
		Instant checkedAt,
		String originKey,
		String destKey,
		TransportMode transportMode,
		LocalDate weekBucket
) {
	public PriceObservationDto {
		if (currency == null || currency.isBlank()) {
			currency = "TRY";
		} else {
			currency = currency.trim().toUpperCase(Locale.ROOT);
		}
	}
}
