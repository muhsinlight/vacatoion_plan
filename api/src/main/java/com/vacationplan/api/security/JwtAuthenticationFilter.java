package com.vacationplan.api.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vacationplan.api.exception.AuthFailureException;
import com.vacationplan.api.service.AuthService;
import com.vacationplan.api.service.AuthSession;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final AuthService authService;
	private final AuthCookies authCookies;
	private final AccessTokenRevocation accessTokenRevocation;

	public JwtAuthenticationFilter(
			JwtService jwtService,
			UserDetailsService userDetailsService,
			AuthService authService,
			AuthCookies authCookies,
			AccessTokenRevocation accessTokenRevocation
	) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.authService = authService;
		this.authCookies = authCookies;
		this.accessTokenRevocation = accessTokenRevocation;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		String path = path(request);
		if (path.startsWith("/api/v1/scraper/")) {
			return true;
		}
		String method = request.getMethod();
		return "POST".equalsIgnoreCase(method)
				&& (path.equals("/api/v1/auth/login")
				|| path.equals("/api/v1/auth/register")
				|| path.equals("/api/v1/auth/refresh"));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		UserPrincipal principal = null;
		boolean blockRefresh = false;
		String access = authCookies.readAccess(request);
		if (access != null) {
			try {
				AccessClaims claims = jwtService.parse(access);
				UserPrincipal loaded = (UserPrincipal) userDetailsService.loadUserByUsername(claims.email());
				if (!accepts(loaded, claims)) {
					blockRefresh = true;
				} else {
					principal = loaded;
				}
			} catch (UsernameNotFoundException ex) {
				principal = null;
			} catch (AuthFailureException | ClassCastException ex) {
				principal = null;
			}
		}
		AuthSession rotated = null;
		if (principal == null && !blockRefresh && !keepsAnonymousSession(request)) {
			String refresh = authCookies.read(request, AuthCookies.REFRESH);
			if (refresh != null) {
				try {
					rotated = authService.rotate(refresh);
					request.setAttribute(AuthCookies.ROTATED, rotated);
					AccessClaims claims = jwtService.parse(rotated.accessToken());
					UserPrincipal loaded = (UserPrincipal) userDetailsService.loadUserByUsername(claims.email());
					if (accepts(loaded, claims)) {
						principal = loaded;
					}
				} catch (AuthFailureException | UsernameNotFoundException ex) {
					rotated = null;
					request.removeAttribute(AuthCookies.ROTATED);
				}
			}
		}
		if (principal != null) {
			SecurityContextHolder.getContext().setAuthentication(
					new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
		}
		try {
			chain.doFilter(request, response);
		} finally {
			if (rotated != null && principal != null && request.getAttribute(AuthCookies.CLEAR) == null) {
				authCookies.write(request, response, rotated);
			}
		}
	}

	private boolean accepts(UserPrincipal loaded, AccessClaims claims) {
		return loaded.isEnabled()
				&& loaded.getId().equals(claims.userId())
				&& loaded.getTokenVersion() == claims.version()
				&& !accessTokenRevocation.isRevoked(claims.jti());
	}

	private static boolean keepsAnonymousSession(HttpServletRequest request) {
		String path = path(request);
		if ("GET".equalsIgnoreCase(request.getMethod())
				&& (path.equals("/api/v1/destinations") || path.startsWith("/api/v1/destinations/"))) {
			return true;
		}
		return path.startsWith("/actuator/")
				|| path.startsWith("/v3/api-docs")
				|| path.startsWith("/swagger-ui")
				|| path.equals("/swagger-ui.html")
				|| path.equals("/error");
	}

	private static String path(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		if (context != null && !context.isEmpty() && uri.startsWith(context)) {
			return uri.substring(context.length());
		}
		return uri;
	}
}
