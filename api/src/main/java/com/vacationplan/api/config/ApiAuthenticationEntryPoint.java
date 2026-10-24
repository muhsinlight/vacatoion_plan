package com.vacationplan.api.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			org.springframework.security.core.AuthenticationException authException
	) throws IOException {
		HttpSecurityResponses.writeJson(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Giriş gerekli");
	}
}
