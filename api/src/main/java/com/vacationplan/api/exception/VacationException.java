package com.vacationplan.api.exception;

/**
 * Uygulama hatası. {@code code} istemciye sabit hata kodu olarak gider.
 */
public abstract class VacationException extends RuntimeException {

	private final String code;

	protected VacationException(String code, String message) {
		this(code, message, null);
	}

	protected VacationException(String code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	/**
	 * İstemciye dönen sabit hata kodu.
	 *
	 * @return hata kodu
	 */
	public String getCode() {
		return code;
	}
}
