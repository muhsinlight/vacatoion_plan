package com.vacationplan.api.dto;

import java.util.List;

import com.vacationplan.api.model.LodgingTier;

public record LodgingPreviewDto(
		String provider,
		MoneyDto perNightRange,
		List<LodgingOfferDto> offers,
		LodgingTier selectedTier
) {
}
