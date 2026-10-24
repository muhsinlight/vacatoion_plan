package com.vacationplan.api.model;

import java.util.List;

public record ResolvedLodging(
		String provider,
		MoneyRange perNightRange,
		List<LodgingOffer> offers,
		LodgingTier selectedTier,
		Status status,
		boolean cityRange
) {
	public enum Status {
		FRESH,
		STALE,
		UNAVAILABLE
	}

	public static ResolvedLodging unavailable() {
		return new ResolvedLodging(null, null, List.of(), null, Status.UNAVAILABLE, false);
	}

	public LodgingOffer selectedOffer() {
		if (offers == null || selectedTier == null) {
			return null;
		}
		return offers.stream()
				.filter(o -> o.tier() == selectedTier)
				.findFirst()
				.orElse(offers.isEmpty() ? null : offers.get(offers.size() / 2));
	}
}
