package com.vacationplan.api.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ApiErrorResponse handleNotMapped(HttpServletRequest request) {
		String path = request.getRequestURI();
		String message = "/".equals(path)
				? "Kök adreste sayfa yok. API /api/v1, dokümantasyon /swagger-ui.html"
				: "Kaynak bulunamadı: " + path;
		return ApiErrorResponse.of("NOT_FOUND", message);
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleMissingHeader(MissingRequestHeaderException ex) {
		return ApiErrorResponse.of("MISSING_HEADER", "Zorunlu başlık eksik: " + ex.getHeaderName());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleStatus(ResponseStatusException ex) {
		String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
		return ResponseEntity.status(ex.getStatusCode()).body(ApiErrorResponse.of("HTTP_" + ex.getStatusCode().value(), reason));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));
		return new ApiErrorResponse("VALIDATION_ERROR", "Geçersiz istek", fields);
	}

	@ExceptionHandler(AuthFailureException.class)
	public ResponseEntity<ApiErrorResponse> handleAuth(AuthFailureException ex) {
		return ResponseEntity.status(ex.getStatus()).body(ApiErrorResponse.of(ex.getCode(), ex.getMessage()));
	}

	@ExceptionHandler(QuotaExceededException.class)
	@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
	public ApiErrorResponse handleQuota(QuotaExceededException ex) {
		return ApiErrorResponse.of(ex.getCode(), ex.getMessage());
	}

	@ExceptionHandler(ActivitySuggestionException.class)
	public ResponseEntity<ApiErrorResponse> handleActivitySuggestion(ActivitySuggestionException ex) {
		return ResponseEntity.status(ex.getStatus()).body(ApiErrorResponse.of(ex.getCode(), ex.getMessage()));
	}

	@ExceptionHandler(VacationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleVacation(VacationException ex) {
		return ApiErrorResponse.of(ex.getCode(), ex.getMessage());
	}

	@ExceptionHandler({ TripNotFoundException.class, DestinationNotFoundException.class })
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ApiErrorResponse handleNotFound(RuntimeException ex) {
		String code = ex instanceof TripNotFoundException trip
				? trip.getCode()
				: ((DestinationNotFoundException) ex).getCode();
		return ApiErrorResponse.of(code, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
		return ApiErrorResponse.of("BAD_REQUEST", ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ApiErrorResponse handleUnexpected(Exception ex) {
		log.error("Beklenmeyen hata", ex);
		return ApiErrorResponse.of("INTERNAL_ERROR", "Beklenmeyen bir hata oluştu.");
	}
}
