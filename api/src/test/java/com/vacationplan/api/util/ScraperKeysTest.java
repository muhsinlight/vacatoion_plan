package com.vacationplan.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ScraperKeysTest {

	private static final String KEY = "k".repeat(ScraperKeys.MIN_LENGTH);

	@Test
	void usableKeyNeedsThirtyTwoCharactersAfterStrip() {
		assertThat(ScraperKeys.isUsable(null)).isFalse();
		assertThat(ScraperKeys.isUsable("  ")).isFalse();
		assertThat(ScraperKeys.isUsable("k".repeat(31))).isFalse();
		assertThat(ScraperKeys.isUsable("  " + KEY + "  ")).isTrue();
	}

	@Test
	void matchRequiresUsableExpectedAndSameBytes() {
		assertThat(ScraperKeys.matches(null, KEY)).isFalse();
		assertThat(ScraperKeys.matches("short", "short")).isFalse();
		assertThat(ScraperKeys.matches(KEY, null)).isFalse();
		assertThat(ScraperKeys.matches(KEY, KEY.substring(1))).isFalse();
		assertThat(ScraperKeys.matches("  " + KEY, KEY + " ")).isTrue();
	}
}
