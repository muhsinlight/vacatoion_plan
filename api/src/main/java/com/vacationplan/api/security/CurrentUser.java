package com.vacationplan.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vacationplan.api.entity.User;
import com.vacationplan.api.exception.AuthFailureException;
import com.vacationplan.api.repository.UserRepository;

import org.springframework.http.HttpStatus;

@Component
public class CurrentUser {

	private final UserRepository userRepository;

	public CurrentUser(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public User requireEntity() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
			throw new AuthFailureException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Giriş gerekli");
		}
		return userRepository.findById(principal.getId())
				.orElseThrow(() -> new AuthFailureException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Giriş gerekli"));
	}
}
