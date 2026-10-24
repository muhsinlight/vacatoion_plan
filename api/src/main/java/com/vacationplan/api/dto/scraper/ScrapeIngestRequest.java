package com.vacationplan.api.dto.scraper;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record ScrapeIngestRequest(
		@NotNull Long targetId,
		@NotNull BigDecimal min,
		@NotNull BigDecimal max,
		String currency,
		Instant fetchedAt
) {
	public ScrapeIngestRequest {
		if (currency == null || currency.isBlank()) {
			currency = "TRY";
		}
	}
}
