package com.vacationplan.api.model;

public enum LodgingTier {
	BUDGET,
	MID,
	PREMIUM;

	public static LodgingTier forTravelStyle(TravelStyle style) {
		if (style == null) {
			return MID;
		}
		return switch (style) {
			case ECONOMIC -> BUDGET;
			case COMFORT -> PREMIUM;
			case BALANCED -> MID;
		};
	}
}
