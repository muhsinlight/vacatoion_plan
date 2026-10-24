package com.vacationplan.api.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ScraperClientConfig {

	@Bean
	RestClient scraperRestClient(RestClient.Builder builder, VacationProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(3))
				.build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(properties.getScraperReadTimeout());
		return builder.clone()
				.baseUrl(trimTrailingSlash(properties.getScraperBaseUrl()))
				.requestFactory(factory)
				.build();
	}

	private static String trimTrailingSlash(String url) {
		if (url == null || url.isBlank()) {
			return "http://localhost:8081";
		}
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
