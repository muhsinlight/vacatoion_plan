package com.vacationplan.api.security;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.vacationplan.api.repository.UserRepository;

@Service
public class SecurityUserService implements UserDetailsService {

	private final UserRepository userRepository;

	public SecurityUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		return userRepository.findByEmail(normalized)
				.map(UserPrincipal::from)
				.orElseThrow(() -> new UsernameNotFoundException(normalized));
	}
}
