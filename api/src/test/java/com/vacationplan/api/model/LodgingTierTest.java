package com.vacationplan.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LodgingTierTest {

	@Test
	void mapsTravelStyle() {
		assertEquals(LodgingTier.BUDGET, LodgingTier.forTravelStyle(TravelStyle.ECONOMIC));
		assertEquals(LodgingTier.MID, LodgingTier.forTravelStyle(TravelStyle.BALANCED));
		assertEquals(LodgingTier.PREMIUM, LodgingTier.forTravelStyle(TravelStyle.COMFORT));
		assertEquals(LodgingTier.MID, LodgingTier.forTravelStyle(null));
	}
}
