package com.vacationplan.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
@Profile("!prod")
public class OpenApiConfig {

	@Bean
	OpenAPI vacationPlanOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Vacation Plan API")
						.description("Tatil bütçesi, günlük program ve optimizasyon önerileri")
						.version("1.0"));
	}
}
