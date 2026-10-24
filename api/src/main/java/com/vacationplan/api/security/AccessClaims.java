package com.vacationplan.api.security;

import java.time.Instant;

public record AccessClaims(Long userId, String email, int version, String jti, Instant expiresAt) {
}
