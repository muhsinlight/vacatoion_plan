package com.vacationplan.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.vacationplan.api.util.ScraperKeys;

class ScraperKeyFilterTest {

	private static final String KEY = "k".repeat(ScraperKeys.MIN_LENGTH);

	@Test
	void rejectsWrongKey() throws Exception {
		MockHttpServletResponse response = run(KEY, "/api/v1/scraper/targets", "wrong");
		assertThat(response.getStatus()).isEqualTo(401);
	}

	@Test
	void rejectsMissingHeader() throws Exception {
		MockHttpServletResponse response = run(KEY, "/api/v1/scraper/observations", null);
		assertThat(response.getStatus()).isEqualTo(401);
	}

	@Test
	void returns503WhenKeyNotConfigured() throws Exception {
		MockHttpServletResponse response = run("", "/api/v1/scraper/ingest", "");
		assertThat(response.getStatus()).isEqualTo(503);
	}

	@Test
	void treatsShortKeyAsNotConfigured() throws Exception {
		MockHttpServletResponse response = run("dev-scraper-key", "/api/v1/scraper/targets", "dev-scraper-key");
		assertThat(response.getStatus()).isEqualTo(503);
	}

	@Test
	void passesMatchingKey() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		MockHttpServletResponse response = run(KEY, "/api/v1/scraper/targets", KEY, chain);
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(chain.getRequest()).isNotNull();
	}

	@Test
	void ignoresUserEndpoints() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		run(KEY, "/api/v1/trips/preview", null, chain);
		assertThat(chain.getRequest()).isNotNull();
	}

	private static MockHttpServletResponse run(String expected, String path, String header) throws Exception {
		return run(expected, path, header, new MockFilterChain());
	}

	private static MockHttpServletResponse run(String expected, String path, String header, MockFilterChain chain)
			throws Exception {
		VacationProperties properties = new VacationProperties();
		properties.setScraperApiKey(expected);
		MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
		if (header != null) {
			request.addHeader(ScraperKeys.HEADER, header);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		new ScraperKeyFilter(properties).doFilter(request, response, chain);
		return response;
	}
}
