package com.vacationplan.api.service;

import com.vacationplan.api.dto.AuthUserResponse;

public record AuthSession(String accessToken, String refreshToken, AuthUserResponse user) {
}
