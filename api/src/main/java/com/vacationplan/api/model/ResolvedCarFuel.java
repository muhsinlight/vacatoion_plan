package com.vacationplan.api.model;

/** Araç yakıtı. {@code note} önizleme varsayımıdır; tutar yoksa {@code amount} null. */
public record ResolvedCarFuel(MoneyRange amount, Status status, String note) {

	public enum Status {
		FRESH,
		UNAVAILABLE
	}
}
