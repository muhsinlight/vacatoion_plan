package com.vacationplan.api.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.service.AuthSession;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Access ve refresh değerleri HttpOnly çerezdedir. Yol {@code /} seçildi;
 * süresi dolan access, aynı istekte refresh çerezini görebilir.
 */
@Component
public class AuthCookies {

	public static final String ACCESS = "access_token";
	public static final String REFRESH = "refresh_token";
	public static final String CLEAR = "auth.clear";
	public static final String ROTATED = "auth.rotated";

	private final VacationProperties properties;

	public AuthCookies(VacationProperties properties) {
		this.properties = properties;
	}

	public void write(HttpServletRequest request, HttpServletResponse response, AuthSession session) {
		boolean secure = request.isSecure();
		add(response, ACCESS, session.accessToken(), properties.getAuth().getAccessTtl().getSeconds(), secure);
		add(response, REFRESH, session.refreshToken(), properties.getAuth().getRefreshTtl().getSeconds(), secure);
	}

	public void clear(HttpServletRequest request, HttpServletResponse response) {
		boolean secure = request.isSecure();
		add(response, ACCESS, "", 0, secure);
		add(response, REFRESH, "", 0, secure);
	}

	public String readAccess(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring("Bearer ".length()).trim();
			if (!token.isEmpty()) {
				return token;
			}
		}
		return read(request, ACCESS);
	}

	public String read(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private static void add(HttpServletResponse response, String name, String value, long maxAgeSeconds, boolean secure) {
		ResponseCookie cookie = ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(secure)
				.path("/")
				.maxAge(maxAgeSeconds)
				.sameSite("Lax")
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
