package com.vacationplan.api.security;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.entity.User;
import com.vacationplan.api.exception.AuthFailureException;

@Component
public class JwtService {

	private final byte[] secret;
	private final Clock clock;
	private final VacationProperties properties;

	public JwtService(VacationProperties properties, Clock clock) {
		byte[] secretBytes = properties.getAuth().getJwtSecret().getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new IllegalStateException("JWT_SECRET en az 32 bayt olmalı");
		}
		this.secret = secretBytes;
		this.clock = clock;
		this.properties = properties;
	}

	public String issueAccess(User user) {
		Instant now = clock.instant();
		Instant exp = now.plus(properties.getAuth().getAccessTtl());
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(user.getId().toString())
				.jwtID(UUID.randomUUID().toString())
				.claim("email", user.getEmail())
				.claim("ver", user.getTokenVersion())
				.issueTime(Date.from(now))
				.expirationTime(Date.from(exp))
				.build();
		try {
			SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
			jwt.sign(new MACSigner(secret));
			return jwt.serialize();
		} catch (JOSEException ex) {
			throw new IllegalStateException("Access token yazılamadı", ex);
		}
	}

	public AccessClaims parse(String token) {
		try {
			SignedJWT jwt = SignedJWT.parse(token);
			if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) {
				throw unauthenticated();
			}
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			Date exp = claims.getExpirationTime();
			if (exp == null || !exp.toInstant().isAfter(clock.instant())) {
				throw unauthenticated();
			}
			String email = claims.getStringClaim("email");
			String jti = claims.getJWTID();
			Object versionClaim = claims.getClaim("ver");
			if (email == null || claims.getSubject() == null || jti == null || jti.isBlank() || !(versionClaim instanceof Number version)) {
				throw unauthenticated();
			}
			return new AccessClaims(Long.valueOf(claims.getSubject()), email, version.intValue(), jti, exp.toInstant());
		} catch (AuthFailureException ex) {
			throw ex;
		} catch (ParseException | JOSEException | NumberFormatException ex) {
			throw unauthenticated();
		}
	}

	private static AuthFailureException unauthenticated() {
		return new AuthFailureException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Giriş gerekli");
	}
}
