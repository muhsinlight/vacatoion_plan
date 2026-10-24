package com.vacationplan.api.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GroqClientConfig {

	@Bean
	RestClient groqRestClient(VacationProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(Duration.ofSeconds(20));
		return RestClient.builder()
				.baseUrl(properties.getGroq().getBaseUrl())
				.requestFactory(factory)
				.build();
	}
}
