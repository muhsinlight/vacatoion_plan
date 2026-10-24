package com.vacationplan.api.config;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Oturumsuz CSRF çerezinin yanıtta yazılması için token'ı çözer.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		if (token == null) {
			token = (CsrfToken) request.getAttribute("_csrf");
		}
		if (token != null) {
			token.getToken();
		}
		chain.doFilter(request, response);
	}
}
