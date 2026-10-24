package com.vacationplan.api.exception;

import org.springframework.http.HttpStatus;

public class ActivitySuggestionException extends VacationException {

	private final HttpStatus status;

	private ActivitySuggestionException(HttpStatus status, String code, String message) {
		this(status, code, message, null);
	}

	private ActivitySuggestionException(HttpStatus status, String code, String message, Throwable cause) {
		super(code, message, cause);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public static ActivitySuggestionException promptUnreadable(Throwable cause) {
		return new ActivitySuggestionException(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"ACTIVITY_SUGGESTION_PROMPT_UNREADABLE",
				"Etkinlik öneri promptu okunamadı.",
				cause);
	}

	public static ActivitySuggestionException missingApiKey() {
		return new ActivitySuggestionException(
				HttpStatus.SERVICE_UNAVAILABLE,
				"ACTIVITY_SUGGESTION_UNAVAILABLE",
				"Etkinlik önerisi için GROQ_API_KEY tanımlı değil.");
	}

	public static ActivitySuggestionException upstreamFailed() {
		return new ActivitySuggestionException(
				HttpStatus.BAD_GATEWAY,
				"ACTIVITY_SUGGESTION_UPSTREAM",
				"Öneri servisi yanıt vermedi.");
	}

	public static ActivitySuggestionException emptyResponse() {
		return invalidResponse("Öneri yanıtı boş.");
	}

	public static ActivitySuggestionException notJsonArray() {
		return invalidResponse("Öneri yanıtı JSON dizisi değil.");
	}

	public static ActivitySuggestionException unreadable() {
		return invalidResponse("Öneri yanıtı okunamadı.");
	}

	public static ActivitySuggestionException itemNotObject() {
		return invalidResponse("Öneri öğesi nesne değil.");
	}

	public static ActivitySuggestionException unexpectedField() {
		return invalidResponse("Öneri yanıtında izin verilmeyen alan var.");
	}

	private static ActivitySuggestionException invalidResponse(String message) {
		return new ActivitySuggestionException(
				HttpStatus.BAD_GATEWAY,
				"ACTIVITY_SUGGESTION_INVALID_RESPONSE",
				message);
	}
}
