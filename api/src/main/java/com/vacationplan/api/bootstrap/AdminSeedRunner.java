package com.vacationplan.api.bootstrap;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vacationplan.api.config.VacationProperties;
import com.vacationplan.api.entity.User;
import com.vacationplan.api.model.UserRole;
import com.vacationplan.api.repository.UserRepository;

/**
 * ADMIN_EMAIL ve ADMIN_PASSWORD doluysa ve e-posta yoksa bir admin yazar.
 */
@Component
public class AdminSeedRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminSeedRunner.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final VacationProperties properties;

	public AdminSeedRunner(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			VacationProperties properties
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		String email = properties.getAuth().getAdminEmail().trim().toLowerCase(Locale.ROOT);
		String password = properties.getAuth().getAdminPassword();
		if (email.isEmpty() || password.isBlank()) {
			return;
		}
		if (password.length() < 8) {
			log.error("Admin hesabı yazılmadı: ADMIN_PASSWORD en az 8 karakter olmalı");
			return;
		}
		User existing = userRepository.findByEmail(email).orElse(null);
		if (existing != null) {
			if (existing.getRole() != UserRole.ADMIN) {
				existing.setRole(UserRole.ADMIN);
				userRepository.save(existing);
				log.info("Mevcut hesap ADMIN yapıldı: {}", email);
			}
			return;
		}
		User admin = new User();
		admin.setEmail(email);
		admin.setFirstName("Sistem");
		admin.setLastName("Yönetici");
		admin.setPasswordHash(passwordEncoder.encode(password));
		admin.setRole(UserRole.ADMIN);
		admin.setEnabled(true);
		userRepository.save(admin);
		log.info("Admin hesabı yazıldı: {}", email);
	}
}
