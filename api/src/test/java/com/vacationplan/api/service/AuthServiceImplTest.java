package com.vacationplan.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.entity.AuthLoginThrottle;
import com.vacationplan.api.entity.AuthRefreshReplay;
import com.vacationplan.api.entity.User;
import com.vacationplan.api.entity.RefreshToken;
import com.vacationplan.api.exception.AuthFailureException;
import com.vacationplan.api.model.UserRole;
import com.vacationplan.api.repository.AuthLoginThrottleRepository;
import com.vacationplan.api.repository.AuthRefreshReplayRepository;
import com.vacationplan.api.repository.UserRepository;
import com.vacationplan.api.repository.RefreshTokenRepository;
import com.vacationplan.api.security.AccessTokenRevocation;
import com.vacationplan.api.security.JwtService;
import com.vacationplan.api.service.impl.AuthServiceImpl;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

	@Mock
	private UserRepository userRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private AuthLoginThrottleRepository authLoginThrottleRepository;

	@Mock
	private AuthRefreshReplayRepository authRefreshReplayRepository;

	@Mock
	private AccessTokenRevocation accessTokenRevocation;

	private final List<RefreshToken> tokens = new ArrayList<>();
	private final Map<String, AuthLoginThrottle> loginThrottleStore = new HashMap<>();
	private final Map<String, AuthRefreshReplay> replayStore = new HashMap<>();
	private final MutableClock clock = new MutableClock(NOW);
	private JwtService jwtService;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		VacationProperties properties = new VacationProperties();
		properties.getAuth().setJwtSecret("test-jwt-secret-must-be-at-least-32b");
		properties.getAuth().setAccessTtl(Duration.ofMinutes(15));
		properties.getAuth().setRefreshTtl(Duration.ofDays(30));
		jwtService = new JwtService(properties, clock);
		authService = new AuthServiceImpl(
				userRepository,
				refreshTokenRepository,
				authLoginThrottleRepository,
				authRefreshReplayRepository,
				new BCryptPasswordEncoder(4),
				jwtService,
				accessTokenRevocation,
				properties,
				clock
		);
		lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(5L);
			return user;
		});
		lenient().when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
			RefreshToken token = invocation.getArgument(0);
			tokens.removeIf(existing -> existing.getTokenHash().equals(token.getTokenHash()));
			tokens.add(token);
			return token;
		});
		lenient().when(refreshTokenRepository.findByTokenHash(any())).thenAnswer(invocation -> tokens.stream()
				.filter(token -> token.getTokenHash().equals(invocation.getArgument(0)))
				.findFirst());
		lenient().when(refreshTokenRepository.revokeIfActive(any(), any())).thenAnswer(invocation -> {
			String hash = invocation.getArgument(0);
			Instant at = invocation.getArgument(1);
			for (RefreshToken token : tokens) {
				if (token.getTokenHash().equals(hash) && token.getRevokedAt() == null) {
					token.setRevokedAt(at);
					return 1;
				}
			}
			return 0;
		});
		lenient().when(authLoginThrottleRepository.findById(any())).thenAnswer(invocation ->
				Optional.ofNullable(loginThrottleStore.get(invocation.getArgument(0))));
		lenient().when(authLoginThrottleRepository.save(any(AuthLoginThrottle.class))).thenAnswer(invocation -> {
			AuthLoginThrottle row = invocation.getArgument(0);
			loginThrottleStore.put(row.getEmail(), row);
			return row;
		});
		lenient().doAnswer(invocation -> {
			loginThrottleStore.remove(invocation.getArgument(0));
			return null;
		}).when(authLoginThrottleRepository).deleteById(any());
		lenient().when(authRefreshReplayRepository.findByPreviousTokenHash(any())).thenAnswer(invocation ->
				Optional.ofNullable(replayStore.get(invocation.getArgument(0))));
		lenient().when(authRefreshReplayRepository.save(any(AuthRefreshReplay.class))).thenAnswer(invocation -> {
			AuthRefreshReplay row = invocation.getArgument(0);
			replayStore.put(row.getPreviousTokenHash(), row);
			return row;
		});
		lenient().doAnswer(invocation -> {
			AuthRefreshReplay row = invocation.getArgument(0);
			replayStore.remove(row.getPreviousTokenHash());
			return null;
		}).when(authRefreshReplayRepository).delete(any(AuthRefreshReplay.class));
		lenient().doAnswer(invocation -> {
			Long userId = invocation.getArgument(0);
			replayStore.entrySet().removeIf(entry -> entry.getValue().getUser().getId().equals(userId));
			return null;
		}).when(authRefreshReplayRepository).deleteByUserId(any());
		lenient().doAnswer(invocation -> {
			Instant cutoff = invocation.getArgument(0);
			replayStore.entrySet().removeIf(entry -> !entry.getValue().getExpiresAt().isAfter(cutoff));
			return null;
		}).when(authRefreshReplayRepository).deleteExpiredBefore(any());
		lenient().when(refreshTokenRepository.revokeActiveByUserId(any(), any())).thenAnswer(invocation -> {
			Long userId = invocation.getArgument(0);
			Instant at = invocation.getArgument(1);
			int count = 0;
			for (RefreshToken token : tokens) {
				if (token.getUser().getId().equals(userId) && token.getRevokedAt() == null) {
					token.setRevokedAt(at);
					count++;
				}
			}
			return count;
		});
	}

	@Test
	void rotateReplaysInsideGraceAndRevokesTheFamilyAfterIt() {
		when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
		AuthSession first = authService.register("muhsin", "ışık", "Ada@Example.com", "password12");
		assertThat(first.user().role()).isEqualTo(UserRole.USER);
		assertThat(first.user().email()).isEqualTo("ada@example.com");
		assertThat(first.user().firstName()).isEqualTo("Muhsin");
		assertThat(first.user().lastName()).isEqualTo("Işık");

		AuthSession second = authService.rotate(first.refreshToken());
		AuthSession replay = authService.rotate(first.refreshToken());
		assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
		assertThat(replay.refreshToken()).isEqualTo(second.refreshToken());
		assertThat(tokens.stream().filter(token -> token.getRevokedAt() == null)).hasSize(1);

		clock.advance(Duration.ofMinutes(1));
		assertThatThrownBy(() -> authService.rotate(first.refreshToken()))
				.isInstanceOf(AuthFailureException.class)
				.hasFieldOrPropertyWithValue("code", "REFRESH_INVALID");
		assertThat(tokens).allMatch(token -> token.getRevokedAt() != null);
	}

	@Test
	void updateProfileStoresThePersonsName() {
		User user = new User();
		user.setId(5L);
		user.setEmail("muhsin@example.com");
		user.setRole(UserRole.USER);
		when(userRepository.findById(5L)).thenReturn(Optional.of(user));

		assertThat(authService.updateProfile(5L, "muhsin", "ışık"))
				.extracting(account -> account.firstName(), account -> account.lastName(), account -> account.email())
				.containsExactly("Muhsin", "Işık", "muhsin@example.com");
	}

	@Test
	void logoutMakesTheSameRefreshInvalidAndRevokesAccess() {
		when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
		AuthSession session = authService.register("Ada", "Lovelace", "ada@example.com", "password12");
		String jti = jwtService.parse(session.accessToken()).jti();
		authService.logoutCurrent(session.refreshToken(), session.accessToken(), null);

		verify(accessTokenRevocation).revokeUntil(eq(jti), eq(jwtService.parse(session.accessToken()).expiresAt()));
		assertThatThrownBy(() -> authService.rotate(session.refreshToken()))
				.isInstanceOf(AuthFailureException.class);
	}

	@Test
	void loginEndsPreviousSessions() {
		when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
		AuthSession first = authService.register("Ada", "Lovelace", "ada@example.com", "password12");
		User user = tokens.get(0).getUser();
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

		AuthSession second = authService.login("ada@example.com", "password12");

		assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
		assertThat(jwtService.parse(second.accessToken()).version()).isEqualTo(jwtService.parse(first.accessToken()).version() + 1);
		assertThatThrownBy(() -> authService.rotate(first.refreshToken()))
				.isInstanceOf(AuthFailureException.class);
		assertThat(tokens.stream().filter(token -> token.getRevokedAt() == null)).hasSize(1);
	}

	@Test
	void repeatedLoginFailuresAreThrottled() {
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
		for (int attempt = 0; attempt < 10; attempt++) {
			assertThatThrownBy(() -> authService.login("ada@example.com", "password12"))
					.hasFieldOrPropertyWithValue("code", "INVALID_CREDENTIALS");
		}
		assertThatThrownBy(() -> authService.login("ada@example.com", "password12"))
				.hasFieldOrPropertyWithValue("code", "LOGIN_THROTTLED");
	}

	@Test
	void unknownEmailAndBadPasswordLookTheSame() {
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> authService.login("ada@example.com", "password12"))
				.isInstanceOf(AuthFailureException.class)
				.hasMessage("E-posta veya şifre hatalı");

		User user = new User();
		user.setId(5L);
		user.setEmail("ada@example.com");
		user.setPasswordHash(new BCryptPasswordEncoder(4).encode("other-pass"));
		user.setRole(UserRole.USER);
		user.setEnabled(true);
		when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
		assertThatThrownBy(() -> authService.login("ada@example.com", "password12"))
				.isInstanceOf(AuthFailureException.class)
				.hasMessage("E-posta veya şifre hatalı");
	}

	private static final class MutableClock extends Clock {

		private Instant current;

		private MutableClock(Instant current) {
			this.current = current;
		}

		private void advance(Duration duration) {
			current = current.plus(duration);
		}

		@Override
		public Instant instant() {
			return current;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
	}
}
