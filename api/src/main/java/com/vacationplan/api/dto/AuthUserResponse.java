package com.vacationplan.api.dto;

import com.vacationplan.api.entity.User;
import com.vacationplan.api.model.UserRole;

public record AuthUserResponse(Long id, String email, String firstName, String lastName, UserRole role) {

	public static AuthUserResponse from(User user) {
		return new AuthUserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole());
	}
}
