package com.vacationplan.api.exception;

public class QuotaExceededException extends VacationException {

	public QuotaExceededException(String message) {
		super("QUOTA_EXCEEDED", message);
	}

	public static QuotaExceededException hourly(int maxPerHour, String label) {
		return new QuotaExceededException(
				"Saatte en fazla %d %s isteği atabilirsiniz.".formatted(maxPerHour, label));
	}
}
