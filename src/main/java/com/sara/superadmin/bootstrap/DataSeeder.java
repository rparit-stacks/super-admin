package com.sara.superadmin.bootstrap;

import com.sara.superadmin.model.SuperAdmin;
import com.sara.superadmin.repository.SuperAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Seeds only the default super-admin account on first boot.
 *
 * <p>Stores are NOT seeded — they are registered manually from the super-admin UI
 * (name + backend domain + API key) and verified via the store's connector handshake.
 * No hardcoded stores, plans, or subscription products.
 */
@Component
public class DataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	private final SuperAdminRepository superAdminRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${super-admin.seed.username}")
	private String seedUsername;
	@Value("${super-admin.seed.password}")
	private String seedPassword;

	public DataSeeder(SuperAdminRepository superAdminRepository, PasswordEncoder passwordEncoder) {
		this.superAdminRepository = superAdminRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		seedSuperAdmin();
	}

	private void seedSuperAdmin() {
		if (superAdminRepository.count() > 0) {
			return;
		}
		superAdminRepository.save(SuperAdmin.builder()
				.username(seedUsername)
				.passwordHash(passwordEncoder.encode(seedPassword))
				.name("Super Admin")
				.enabled(true)
				.createdAt(Instant.now())
				.build());
		log.info("Seeded default super-admin '{}'", seedUsername);
	}
}
