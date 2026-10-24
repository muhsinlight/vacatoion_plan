package com.vacationplan.api.dto.scraper;

import java.util.List;

public record PriceObservationBatchResponse(
		int saved,
		int routeQuotesUpdated,
		List<Long> observationIds
) {
}
