package com.vacationplan.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vacationplan.api.dto.AuthUserResponse;
import com.vacationplan.api.dto.LoginRequest;
import com.vacationplan.api.dto.ProfileUpdateRequest;
import com.vacationplan.api.dto.RegisterRequest;
import com.vacationplan.api.security.AuthCookies;
import com.vacationplan.api.security.CurrentUser;
import com.vacationplan.api.service.AuthService;
import com.vacationplan.api.service.AuthSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * JSON oturum uçları. Token gövdede dönmez; çerez yazılır.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final AuthCookies authCookies;
	private final CurrentUser currentUser;

	public AuthController(AuthService authService, AuthCookies authCookies, CurrentUser currentUser) {
		this.authService = authService;
		this.authCookies = authCookies;
		this.currentUser = currentUser;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthUserResponse register(
			@Valid @RequestBody RegisterRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse response
	) {
		AuthSession session = authService.register(
				request.firstName(),
				request.lastName(),
				request.email(),
				request.password()
		);
		authCookies.write(httpRequest, response, session);
		return session.user();
	}

	@PostMapping("/login")
	public AuthUserResponse login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse response
	) {
		AuthSession session = authService.login(request.email(), request.password());
		authCookies.write(httpRequest, response, session);
		return session.user();
	}

	@PostMapping("/refresh")
	public AuthUserResponse refresh(HttpServletRequest request, HttpServletResponse response) {
		AuthSession session = authService.rotate(authCookies.read(request, AuthCookies.REFRESH));
		authCookies.write(request, response, session);
		return session.user();
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		end(request, response, false);
	}

	@PostMapping("/logout-all")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logoutAll(HttpServletRequest request, HttpServletResponse response) {
		end(request, response, true);
	}

	@GetMapping("/me")
	public AuthUserResponse me() {
		return AuthUserResponse.from(currentUser.requireEntity());
	}

	@PutMapping("/profile")
	public AuthUserResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
		return authService.updateProfile(currentUser.requireEntity().getId(), request.firstName(), request.lastName());
	}

	private void end(HttpServletRequest request, HttpServletResponse response, boolean all) {
		request.setAttribute(AuthCookies.CLEAR, Boolean.TRUE);
		if (all) {
			authService.revokeAll(currentUser.requireEntity().getId());
		} else {
			AuthSession rotated = (AuthSession) request.getAttribute(AuthCookies.ROTATED);
			String raw = rotated == null ? authCookies.read(request, AuthCookies.REFRESH) : rotated.refreshToken();
			String rotatedAccess = rotated == null ? null : rotated.accessToken();
			authService.logoutCurrent(raw, authCookies.readAccess(request), rotatedAccess);
		}
		authCookies.clear(request, response);
	}
}
