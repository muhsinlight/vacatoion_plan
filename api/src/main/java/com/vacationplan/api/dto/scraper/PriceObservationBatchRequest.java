package com.vacationplan.api.dto.scraper;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record PriceObservationBatchRequest(
		@NotEmpty
		@Size(max = PriceObservationBatchRequest.MAX_OBSERVATIONS, message = "En fazla 200 gözlem gönderilebilir")
		List<@Valid PriceObservationDto> observations
) {
	public static final int MAX_OBSERVATIONS = 200;
}
