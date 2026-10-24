package com.vacationplan.api.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vacationplan.api.util.ScraperKeys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class ScraperKeyFilter extends OncePerRequestFilter {

	static final String PATH_PREFIX = "/api/v1/scraper/";

	private final VacationProperties properties;

	public ScraperKeyFilter(VacationProperties properties) {
		this.properties = properties;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return !path.startsWith(PATH_PREFIX) || "OPTIONS".equalsIgnoreCase(request.getMethod());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String expected = properties.getScraperApiKey();
		if (!ScraperKeys.isUsable(expected)) {
			reject(response, HttpStatus.SERVICE_UNAVAILABLE, "SCRAPER_KEY_MISSING", "Scraper API anahtarı yapılandırılmamış");
			return;
		}
		if (!ScraperKeys.matches(expected, request.getHeader(ScraperKeys.HEADER))) {
			reject(response, HttpStatus.UNAUTHORIZED, "SCRAPER_KEY_INVALID", "Geçersiz scraper anahtarı");
			return;
		}
		var allowedIps = properties.getScraperAllowedIps();
		if (allowedIps != null && !allowedIps.isEmpty()) {
			String ip = request.getRemoteAddr();
			if (ip == null || allowedIps.stream().noneMatch(ip::equals)) {
				reject(response, HttpStatus.FORBIDDEN, "SCRAPER_IP_DENIED", "Bu adresten scraper yazımı yok");
				return;
			}
		}
		chain.doFilter(request, response);
	}

	private static void reject(HttpServletResponse response, HttpStatus status, String code, String message)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
	}
}
