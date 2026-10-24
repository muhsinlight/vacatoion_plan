package com.vacationplan.api.exception;

public class TripNotFoundException extends RuntimeException {

	private final String code = "TRIP_NOT_FOUND";

	public TripNotFoundException(Long id) {
		super("Seyahat bulunamadı: " + id);
	}

	public String getCode() {
		return code;
	}
}
