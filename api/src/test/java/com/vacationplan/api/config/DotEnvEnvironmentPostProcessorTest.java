package com.vacationplan.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DotEnvEnvironmentPostProcessorTest {

	@Test
	void parsesKeyValueCommentsAndQuotes() {
		Map<String, Object> values = DotEnvEnvironmentPostProcessor.parse(List.of(
				"# comment",
				"",
				"GROQ_API_KEY=gsk_test",
				"export OTHER='quoted'",
				"SPACED = \"also quoted\"",
				"bad line",
				"1INVALID=nope"
		));

		assertEquals("gsk_test", values.get("GROQ_API_KEY"));
		assertEquals("quoted", values.get("OTHER"));
		assertEquals("also quoted", values.get("SPACED"));
		assertFalse(values.containsKey("1INVALID"));
		assertEquals(3, values.size());
	}

	@Test
	void ignoresUtf8BomOnFirstLine() {
		Map<String, Object> values = DotEnvEnvironmentPostProcessor.parse(List.of("\uFEFFSCRAPER_API_KEY=abc"));

		assertEquals("abc", values.get("SCRAPER_API_KEY"));
	}
}
