package com.vacationplan.api.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.dto.AuthUserResponse;
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
import com.vacationplan.api.security.AccessClaims;
import com.vacationplan.api.security.AccessTokenRevocation;
import com.vacationplan.api.security.JwtService;
import com.vacationplan.api.service.AuthService;
import com.vacationplan.api.service.AuthSession;

@Service
public class AuthServiceImpl implements AuthService {

	/** Paralel meşru istek ile geç gelen çalıntı refresh'i ayıran pencere. */
	static final Duration REUSE_GRACE = Duration.ofSeconds(10);

	private static final int MAX_LOGIN_FAILURES = 10;
	private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(15);

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthLoginThrottleRepository authLoginThrottleRepository;
	private final AuthRefreshReplayRepository authRefreshReplayRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AccessTokenRevocation accessTokenRevocation;
	private final VacationProperties properties;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();
	private final String dummyHash;
	private final ConcurrentHashMap<String, AuthSession> replaySessions = new ConcurrentHashMap<>();

	public AuthServiceImpl(
			UserRepository userRepository,
			RefreshTokenRepository refreshTokenRepository,
			AuthLoginThrottleRepository authLoginThrottleRepository,
			AuthRefreshReplayRepository authRefreshReplayRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			AccessTokenRevocation accessTokenRevocation,
			VacationProperties properties,
			Clock clock
	) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.authLoginThrottleRepository = authLoginThrottleRepository;
		this.authRefreshReplayRepository = authRefreshReplayRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.accessTokenRevocation = accessTokenRevocation;
		this.properties = properties;
		this.clock = clock;
		this.dummyHash = passwordEncoder.encode("invalid-login-padding");
	}

	@Override
	@Transactional
	public AuthSession register(String firstName, String lastName, String email, String password) {
		String normalized = normalize(email);
		ensureLoginAllowed(normalized);
		if (userRepository.existsByEmail(normalized)) {
			passwordEncoder.matches(password, dummyHash);
			recordLoginFailure(normalized);
			throw new AuthFailureException(HttpStatus.BAD_REQUEST, "REGISTER_REJECTED", "Kayıt tamamlanamadı");
		}
		User user = new User();
		user.setFirstName(titleCase(firstName));
		user.setLastName(titleCase(lastName));
		user.setEmail(normalized);
		user.setPasswordHash(passwordEncoder.encode(password));
		user.setRole(UserRole.USER);
		user.setEnabled(true);
		return issue(userRepository.save(user));
	}

	@Override
	@Transactional
	public AuthUserResponse updateProfile(Long userId, String firstName, String lastName) {
		User user = userRepository.findById(userId).orElseThrow(AuthServiceImpl::invalid);
		user.setFirstName(titleCase(firstName));
		user.setLastName(titleCase(lastName));
		return AuthUserResponse.from(userRepository.save(user));
	}

	@Override
	@Transactional
	public AuthSession login(String email, String password) {
		String normalized = normalize(email);
		ensureLoginAllowed(normalized);
		User user = userRepository.findByEmail(normalized).orElse(null);
		String hash = user == null ? dummyHash : user.getPasswordHash();
		boolean passwordOk = passwordEncoder.matches(password, hash);
		if (user == null || !user.isEnabled() || !passwordOk) {
			recordLoginFailure(normalized);
			throw new AuthFailureException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "E-posta veya şifre hatalı");
		}
		authLoginThrottleRepository.deleteById(normalized);
		endSessions(user);
		return issue(user);
	}

	@Override
	@Transactional
	public AuthSession rotate(String rawRefresh) {
		if (rawRefresh == null || rawRefresh.isBlank()) {
			throw invalid();
		}
		String hash = sha256(rawRefresh);
		AuthSession replay = liveReplay(hash);
		if (replay != null) {
			return replay;
		}
		int updated = refreshTokenRepository.revokeIfActive(hash, clock.instant());
		RefreshToken current = refreshTokenRepository.findByTokenHash(hash).orElseThrow(AuthServiceImpl::invalid);
		if (!current.getUser().isEnabled()) {
			endSessions(current.getUser());
			throw invalid();
		}
		if (updated == 1) {
			if (!current.getExpiresAt().isAfter(clock.instant())) {
				throw invalid();
			}
			AuthSession session = issue(current.getUser());
			remember(hash, session, current.getUser());
			return session;
		}
		replay = liveReplay(hash);
		if (replay != null) {
			return replay;
		}
		Instant revokedAt = current.getRevokedAt();
		if (revokedAt == null || revokedAt.plus(REUSE_GRACE).isAfter(clock.instant())) {
			throw invalid();
		}
		endSessions(current.getUser());
		throw invalid();
	}

	@Override
	@Transactional
	public void revoke(String rawRefresh) {
		if (rawRefresh == null || rawRefresh.isBlank()) {
			return;
		}
		refreshTokenRepository.findByTokenHash(sha256(rawRefresh)).ifPresent(token -> {
			if (token.getRevokedAt() == null) {
				token.setRevokedAt(clock.instant());
			}
		});
	}

	@Override
	@Transactional
	public void logoutCurrent(String rawRefresh, String presentedAccess, String rotatedAccess) {
		revoke(rawRefresh);
		revokeAccess(presentedAccess);
		if (rotatedAccess != null && !rotatedAccess.equals(presentedAccess)) {
			revokeAccess(rotatedAccess);
		}
	}

	@Override
	@Transactional
	public void revokeAll(Long userId) {
		userRepository.findById(userId).ifPresent(this::endSessions);
	}

	private AuthSession issue(User user) {
		String raw = newRaw();
		RefreshToken token = new RefreshToken();
		token.setUser(user);
		token.setTokenHash(sha256(raw));
		token.setExpiresAt(clock.instant().plus(properties.getAuth().getRefreshTtl()));
		refreshTokenRepository.save(token);
		return new AuthSession(jwtService.issueAccess(user), raw, AuthUserResponse.from(user));
	}

	private String newRaw() {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String sha256(String raw) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void endSessions(User user) {
		authRefreshReplayRepository.deleteByUserId(user.getId());
		refreshTokenRepository.revokeActiveByUserId(user.getId(), clock.instant());
		user.setTokenVersion(user.getTokenVersion() + 1);
		userRepository.save(user);
	}

	private void revokeAccess(String rawAccess) {
		if (rawAccess == null || rawAccess.isBlank()) {
			return;
		}
		try {
			AccessClaims claims = jwtService.parse(rawAccess);
			accessTokenRevocation.revokeUntil(claims.jti(), claims.expiresAt());
		} catch (AuthFailureException ex) {
			// Süresi dolmuş access zaten geçersiz.
		}
	}

	private void remember(String previousHash, AuthSession session, User user) {
		Instant now = clock.instant();
		authRefreshReplayRepository.deleteExpiredBefore(now);
		AuthRefreshReplay replay = new AuthRefreshReplay();
		replay.setPreviousTokenHash(previousHash);
		replay.setUser(user);
		replay.setAccessToken(sha256(session.accessToken()));
		replay.setRefreshToken(sha256(session.refreshToken()));
		replay.setExpiresAt(now.plus(REUSE_GRACE));
		authRefreshReplayRepository.save(replay);
		replaySessions.put(previousHash, session);
	}

	private AuthSession liveReplay(String previousHash) {
		Instant now = clock.instant();
		Optional<AuthRefreshReplay> stored = authRefreshReplayRepository.findByPreviousTokenHash(previousHash);
		if (stored.isEmpty() || !stored.get().getExpiresAt().isAfter(now)) {
			stored.ifPresent(replay -> authRefreshReplayRepository.delete(replay));
			replaySessions.remove(previousHash);
			return null;
		}
		AuthRefreshReplay replay = stored.get();
		AuthSession session = replaySessions.get(previousHash);
		if (session == null
				|| !replay.getAccessToken().equals(sha256(session.accessToken()))
				|| !replay.getRefreshToken().equals(sha256(session.refreshToken()))) {
			return null;
		}
		Optional<RefreshToken> successor = refreshTokenRepository.findByTokenHash(sha256(session.refreshToken()));
		if (successor.isEmpty()
				|| successor.get().getRevokedAt() != null
				|| !successor.get().getExpiresAt().isAfter(now)
				|| !successor.get().getUser().isEnabled()) {
			authRefreshReplayRepository.delete(replay);
			replaySessions.remove(previousHash);
			return null;
		}
		return session;
	}

	private void ensureLoginAllowed(String email) {
		authLoginThrottleRepository.findById(email).ifPresent(window -> {
			if (window.getFailureCount() >= MAX_LOGIN_FAILURES
					&& window.getWindowStart().plus(LOGIN_FAILURE_WINDOW).isAfter(clock.instant())) {
				throw new AuthFailureException(
						HttpStatus.TOO_MANY_REQUESTS,
						"LOGIN_THROTTLED",
						"Çok fazla deneme. Bir süre sonra yeniden deneyin");
			}
		});
	}

	private void recordLoginFailure(String email) {
		Instant now = clock.instant();
		AuthLoginThrottle window = authLoginThrottleRepository.findById(email).orElse(null);
		if (window == null || !window.getWindowStart().plus(LOGIN_FAILURE_WINDOW).isAfter(now)) {
			window = new AuthLoginThrottle();
			window.setEmail(email);
			window.setWindowStart(now);
			window.setFailureCount(1);
		} else {
			window.setFailureCount(window.getFailureCount() + 1);
		}
		authLoginThrottleRepository.save(window);
	}

	private static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String titleCase(String value) {
		Locale turkish = Locale.forLanguageTag("tr");
		String[] parts = value.trim().replaceAll("\\s+", " ").split(" ");
		StringBuilder builder = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(' ');
			}
			String lower = part.toLowerCase(turkish);
			int first = lower.offsetByCodePoints(0, 1);
			builder.append(lower.substring(0, first).toUpperCase(turkish)).append(lower.substring(first));
		}
		return builder.toString();
	}

	private static AuthFailureException invalid() {
		return new AuthFailureException(HttpStatus.UNAUTHORIZED, "REFRESH_INVALID", "Oturum yenilenemedi");
	}

}
