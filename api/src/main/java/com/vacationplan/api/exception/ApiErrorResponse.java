package com.vacationplan.api.exception;

import java.util.Map;

public record ApiErrorResponse(
		String code,
		String message,
		Map<String, String> fields
) {
	public static ApiErrorResponse of(String code, String message) {
		return new ApiErrorResponse(code, message, null);
	}
}
