package com.vacationplan.api.dto;

import java.math.BigDecimal;
import java.util.List;

import com.vacationplan.api.model.LodgingTier;

public record LodgingOfferDto(
		LodgingTier tier,
		String provider,
		String name,
		Integer stars,
		BigDecimal rating,
		String boardType,
		List<String> features,
		BigDecimal pricePerNight,
		String url,
		String imageUrl
) {
	public static LodgingOfferDto from(com.vacationplan.api.model.LodgingOffer offer) {
		return new LodgingOfferDto(
				offer.tier(),
				offer.provider(),
				offer.name(),
				offer.stars(),
				offer.rating(),
				offer.boardType(),
				offer.features(),
				offer.pricePerNight(),
				offer.url(),
				offer.imageUrl()
		);
	}
}
