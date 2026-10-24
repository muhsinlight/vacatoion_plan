package com.vacationplan.api.dto.scraper;

import java.time.LocalDate;

public record BusScrapeLeg(
		String origin,
		String dest,
		LocalDate travelDate
) {
}
