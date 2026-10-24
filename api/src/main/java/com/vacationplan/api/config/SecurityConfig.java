package com.vacationplan.api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import com.vacationplan.api.security.JwtAuthenticationFilter;

/**
 * Oturum yoktur. JSON uçları CSRF çerezi taşır. Scraper uçları anahtar filtresindedir.
 * Kimlik, Bearer başlığı veya access çerezindeki JWT ile kurulur.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
		FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			VacationProperties properties,
			JwtAuthenticationFilter jwtAuthenticationFilter
	) throws Exception {
		CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfRepository.setHeaderName("X-CSRF-TOKEN");
		CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
		csrfHandler.setCsrfRequestAttributeName("_csrf");

		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfRepository)
						.csrfTokenRequestHandler(csrfHandler)
						.ignoringRequestMatchers("/api/v1/scraper/**"))
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.logout(logout -> logout.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(new ApiAuthenticationEntryPoint())
						.accessDeniedHandler(new ApiAccessDeniedHandler()))
				.addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)
				.addFilterBefore(new ScraperKeyFilter(properties), JwtAuthenticationFilter.class)
				.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/actuator/health",
								"/actuator/info",
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/error")
						.permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh")
						.permitAll()
						.requestMatchers("/api/v1/scraper/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/destinations", "/api/v1/destinations/**").permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/v1/**").authenticated()
						.anyRequest().denyAll());
		return http.build();
	}
}
