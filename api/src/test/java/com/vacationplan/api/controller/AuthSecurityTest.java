package com.vacationplan.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import com.vacationplan.api.dto.AuthUserResponse;
import com.vacationplan.api.dto.BudgetPreviewDto;
import com.vacationplan.api.dto.MoneyDto;
import com.vacationplan.api.dto.TripPreviewResponse;
import com.vacationplan.api.entity.User;
import com.vacationplan.api.model.UserRole;
import com.vacationplan.api.repository.UserRepository;
import com.vacationplan.api.security.AccessTokenRevocation;
import com.vacationplan.api.security.AuthCookies;
import com.vacationplan.api.security.CurrentUser;
import com.vacationplan.api.security.JwtAuthenticationFilter;
import com.vacationplan.api.security.JwtService;
import com.vacationplan.api.security.SecurityUserService;
import com.vacationplan.api.service.AuthService;
import com.vacationplan.api.service.AuthSession;
import com.vacationplan.api.service.TripPreviewService;
import com.vacationplan.api.service.TripService;

@WebMvcTest(controllers = {
		TripController.class,
		AuthController.class,
		AdminController.class
})
@Import({
		SecurityConfig.class,
		JwtService.class,
		JwtAuthenticationFilter.class,
		AuthCookies.class,
		SecurityUserService.class,
		TimeConfig.class
})
@EnableConfigurationProperties(VacationProperties.class)
@TestPropertySource(properties = {
		"vacation.auth.jwt-secret=test-jwt-secret-must-be-at-least-32b",
		"vacation.auth.access-ttl=15m",
		"vacation.auth.refresh-ttl=30d"
})
@TestConstructor(autowireMode = AutowireMode.ALL)
class AuthSecurityTest {

	private final MockMvc mockMvc;

	private final JwtService jwtService;

	AuthSecurityTest(MockMvc mockMvc, JwtService jwtService) {
		this.mockMvc = mockMvc;
		this.jwtService = jwtService;
	}

	@MockitoBean
	private TripPreviewService tripPreviewService;

	@MockitoBean
	private TripService tripService;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private CurrentUser currentUser;

	@MockitoBean
	private AccessTokenRevocation accessTokenRevocation;

	@Test
	void anonymousTripPreviewIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/v1/trips/preview")
						.contentType(MediaType.APPLICATION_JSON)
						.content(tripJson())
						.with(csrf()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void bearerPreviewReturnsBudget() throws Exception {
		stubUser(UserRole.USER);
		when(tripPreviewService.preview(any())).thenReturn(preview());

		mockMvc.perform(post("/api/v1/trips/preview")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(tripJson())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.budget.total.min").value(10));
	}

	@Test
	void userCannotOpenAdminUsers() throws Exception {
		stubUser(UserRole.USER);
		mockMvc.perform(get("/api/v1/admin/users")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void adminCanListUsers() throws Exception {
		stubUser(UserRole.ADMIN);
		when(userRepository.findAll(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(user(UserRole.ADMIN))));

		mockMvc.perform(get("/api/v1/admin/users")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].email").value("ada@example.com"))
				.andExpect(jsonPath("$.page").value(0));
	}

	@Test
	void loginWithoutCsrfIsForbiddenAndWithCsrfSetsCookies() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson()))
				.andExpect(status().isForbidden());

		when(authService.login("ada@example.com", "password12"))
				.thenReturn(new AuthSession("access-token", "refresh-token", new AuthUserResponse(7L, "ada@example.com", "Ada", "Lovelace", UserRole.USER)));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("ada@example.com"))
				.andExpect(cookie().exists(AuthCookies.ACCESS))
				.andExpect(cookie().exists(AuthCookies.REFRESH))
				.andExpect(cookie().httpOnly(AuthCookies.ACCESS, true))
				.andExpect(cookie().httpOnly(AuthCookies.REFRESH, true))
				.andExpect(cookie().secure(AuthCookies.ACCESS, false))
				.andExpect(cookie().secure(AuthCookies.REFRESH, false));
	}

	@Test
	void httpsLoginSetsSecureCookies() throws Exception {
		when(authService.login("ada@example.com", "password12"))
				.thenReturn(new AuthSession("access-token", "refresh-token", new AuthUserResponse(7L, "ada@example.com", "Ada", "Lovelace", UserRole.USER)));

		mockMvc.perform(post("/api/v1/auth/login")
						.secure(true)
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson())
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(cookie().secure(AuthCookies.ACCESS, true))
				.andExpect(cookie().secure(AuthCookies.REFRESH, true));
	}

	@Test
	void staleAccessVersionIsUnauthorized() throws Exception {
		User user = user(UserRole.USER);
		String token = jwtService.issueAccess(user);
		user.setTokenVersion(1);
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

		mockMvc.perform(get("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void revokedAccessIsUnauthorized() throws Exception {
		stubUser(UserRole.USER);
		when(accessTokenRevocation.isRevoked(any())).thenReturn(true);

		mockMvc.perform(get("/api/v1/auth/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutClearsCookies() throws Exception {
		stubUser(UserRole.USER);
		mockMvc.perform(post("/api/v1/auth/logout")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
						.cookie(new jakarta.servlet.http.Cookie(AuthCookies.REFRESH, "raw-refresh"))
						.with(csrf()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge(AuthCookies.ACCESS, 0))
				.andExpect(cookie().maxAge(AuthCookies.REFRESH, 0));
	}

	private void stubUser(UserRole role) {
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user(role)));
	}

	private String accessToken() {
		return jwtService.issueAccess(user(UserRole.USER));
	}

	private static User user(UserRole role) {
		User user = new User();
		user.setId(7L);
		user.setEmail("ada@example.com");
		user.setPasswordHash("hash");
		user.setRole(role);
		user.setEnabled(true);
		return user;
	}

	private static TripPreviewResponse preview() {
		var budget = new BudgetPreviewDto(
				new MoneyDto(BigDecimal.TEN, BigDecimal.valueOf(20), "TRY"),
				new MoneyDto(BigDecimal.valueOf(5), BigDecimal.TEN, "TRY"),
				new MoneyDto(BigDecimal.valueOf(3), BigDecimal.valueOf(6), "TRY"),
				List.of()
		);
		return new TripPreviewResponse(List.of(), budget, List.of(), List.of(), "test", null, List.of(), List.of());
	}

	private static String loginJson() {
		return """
				{"email":"ada@example.com","password":"password12"}
				""";
	}

	private static String tripJson() {
		return """
				{
				  "originCity": "Aksaray",
				  "destinationCity": "Isparta",
				  "startDate": "2026-07-10",
				  "endDate": "2026-07-12",
				  "adults": 2
				}
				""";
	}
}
