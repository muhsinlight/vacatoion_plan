package com.vacationplan.api.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.vacationplan.api.entity.User;
import com.vacationplan.api.model.UserRole;

public class UserPrincipal implements UserDetails {

	private final Long id;
	private final String email;
	private final String passwordHash;
	private final UserRole role;
	private final boolean enabled;
	private final int tokenVersion;

	public UserPrincipal(Long id, String email, String passwordHash, UserRole role, boolean enabled, int tokenVersion) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.enabled = enabled;
		this.tokenVersion = tokenVersion;
	}

	public static UserPrincipal from(User user) {
		return new UserPrincipal(
				user.getId(),
				user.getEmail(),
				user.getPasswordHash(),
				user.getRole(),
				user.isEnabled(),
				user.getTokenVersion());
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public UserRole getRole() {
		return role;
	}

	public int getTokenVersion() {
		return tokenVersion;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
