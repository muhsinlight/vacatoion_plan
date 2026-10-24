package com.vacationplan.api.dto.scraper;

public record ScrapeIngestResponse(
		long targetId,
		String cityKey,
		boolean catalogUpdated,
		long priceRecordId
) {
}
