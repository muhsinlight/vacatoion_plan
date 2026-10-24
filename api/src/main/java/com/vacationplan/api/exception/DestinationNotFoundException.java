package com.vacationplan.api.exception;

public class DestinationNotFoundException extends RuntimeException {

	private final String code = "DESTINATION_NOT_FOUND";

	public DestinationNotFoundException(String cityKey) {
		super("Destinasyon bulunamadı: " + cityKey);
	}

	public String getCode() {
		return code;
	}
}
