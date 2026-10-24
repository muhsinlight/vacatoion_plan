package com.vacationplan.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PostgresSchemaIT {

	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

	@DynamicPropertySource
	static void datasource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.docker.compose.enabled", () -> "false");
		registry.add("vacation.auth.jwt-secret", () -> "test-jwt-secret-must-be-at-least-32b");
	}

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void liquibaseAppliesTheChangelog() {
		Integer changes = jdbc.queryForObject("select count(*) from databasechangelog", Integer.class);
		Integer destinations = jdbc.queryForObject(
				"select count(*) from information_schema.tables where table_schema = 'public' and table_name = 'destinations'",
				Integer.class);

		assertThat(changes).isGreaterThanOrEqualTo(5);
		assertThat(destinations).isEqualTo(1);
	}
}
