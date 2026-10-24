package com.vacationplan.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.vacationplan.api.config.SecurityConfig;
import com.vacationplan.api.config.TimeConfig;
import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.dto.BudgetPreviewDto;
import com.vacationplan.api.dto.MoneyDto;
import com.vacationplan.api.dto.TripPreviewResponse;
import com.vacationplan.api.entity.User;
import com.vacationplan.api.model.UserRole;
import com.vacationplan.api.repository.UserRepository;
import com.vacationplan.api.security.AccessTokenRevocation;
import com.vacationplan.api.security.AuthCookies;
import com.vacationplan.api.security.JwtAuthenticationFilter;
import com.vacationplan.api.security.JwtService;
import com.vacationplan.api.security.SecurityUserService;
import com.vacationplan.api.service.AuthService;
import com.vacationplan.api.service.TripPreviewService;
import com.vacationplan.api.service.TripService;

@WebMvcTest(TripController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, AuthCookies.class, SecurityUserService.class, TimeConfig.class})
@EnableConfigurationProperties(VacationProperties.class)
@TestPropertySource(properties = {
		"vacation.auth.jwt-secret=test-jwt-secret-must-be-at-least-32b",
		"vacation.auth.access-ttl=15m",
		"vacation.auth.refresh-ttl=30d"
})
@TestConstructor(autowireMode = AutowireMode.ALL)
class TripControllerTest {

	private final MockMvc mockMvc;

	private final JwtService jwtService;

	TripControllerTest(MockMvc mockMvc, JwtService jwtService) {
		this.mockMvc = mockMvc;
		this.jwtService = jwtService;
	}

	@MockitoBean
	private TripPreviewService tripPreviewService;

	@MockitoBean
	private TripService tripService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private AccessTokenRevocation accessTokenRevocation;

	@Test
	void previewReturnsBudgetSummary() throws Exception {
		var budget = new BudgetPreviewDto(
				new MoneyDto(java.math.BigDecimal.TEN, java.math.BigDecimal.valueOf(20), "TRY"),
				new MoneyDto(java.math.BigDecimal.valueOf(5), java.math.BigDecimal.TEN, "TRY"),
				new MoneyDto(java.math.BigDecimal.valueOf(3), java.math.BigDecimal.valueOf(6), "TRY"),
				List.of()
		);
		when(tripPreviewService.preview(any())).thenReturn(new TripPreviewResponse(List.of(), budget, List.of(), List.of(), "test", null, List.of(), List.of()));
		User user = new User();
		user.setId(7L);
		user.setEmail("ada@example.com");
		user.setPasswordHash("hash");
		user.setRole(UserRole.USER);
		user.setEnabled(true);
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

		mockMvc.perform(post("/api/v1/trips/preview")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueAccess(user))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "originCity": "Aksaray",
								  "destinationCity": "Isparta",
								  "startDate": "2026-07-10",
								  "endDate": "2026-07-12",
								  "adults": 2
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.budget.total.min").value(10));
	}

	@Test
	void returnBeforeDepartureIs400OnEndDate() throws Exception {
		User user = user();
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

		mockMvc.perform(post("/api/v1/trips/preview")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueAccess(user))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "originCity": "Aksaray",
								  "destinationCity": "Isparta",
								  "startDate": "2026-07-12",
								  "endDate": "2026-07-10",
								  "adults": 2
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.endDate").exists());
	}

	@Test
	void childAgeOutsideRangeIs400() throws Exception {
		User user = user();
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

		mockMvc.perform(post("/api/v1/trips/preview")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issueAccess(user))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "originCity": "Aksaray",
								  "destinationCity": "Isparta",
								  "startDate": "2026-07-10",
								  "endDate": "2026-07-12",
								  "adults": 2,
								  "childAges": [18]
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields['childAges[0]']").exists());
	}

	private static User user() {
		User user = new User();
		user.setId(7L);
		user.setEmail("ada@example.com");
		user.setPasswordHash("hash");
		user.setRole(UserRole.USER);
		user.setEnabled(true);
		return user;
	}
}
