package com.vacationplan.api.model;

public enum ScrapeCatalogField {
	BUS_ROUND_TRIP,
	/** Canlı gece fiyatı yalnız lodging_offers üzerinden gelir; katalog değeri tohum ve yedek tahmindir. */
	LODGING_PER_NIGHT,
	FOOD_PER_PERSON_DAY,
	LOCAL_TRANSPORT_DAY,
	ACTIVITY_ENTRY;

	public boolean acceptsScrape() {
		return this != LODGING_PER_NIGHT;
	}
}
