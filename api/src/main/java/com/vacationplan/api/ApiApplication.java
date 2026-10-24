package com.vacationplan.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.vacationplan.api.config.VacationProperties;

/**
 * API sürecinin giriş noktası.
 *
 * <p>{@code vacation.*} özellikleri {@link VacationProperties} üzerinden bağlanır.
 */
@SpringBootApplication
@EnableConfigurationProperties(VacationProperties.class)
public class ApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
