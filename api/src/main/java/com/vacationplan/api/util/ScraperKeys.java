package com.vacationplan.api.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class ScraperKeys {

	public static final String HEADER = "X-Scraper-Key";

	
	public static final int MIN_LENGTH = 32;

	private ScraperKeys() {
	}

	public static boolean isUsable(String key) {
		return key != null && key.strip().length() >= MIN_LENGTH;
	}

	public static boolean matches(String expected, String provided) {
		if (!isUsable(expected) || provided == null) {
			return false;
		}
		return MessageDigest.isEqual(
				expected.strip().getBytes(StandardCharsets.UTF_8),
				provided.strip().getBytes(StandardCharsets.UTF_8));
	}
}
