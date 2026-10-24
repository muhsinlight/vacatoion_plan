package com.vacationplan.api.dto.scraper;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BusRouteBatchResponse(int saved, List<String> errors) {
	public BusRouteBatchResponse {
		errors = errors == null ? List.of() : List.copyOf(errors);
	}
}
