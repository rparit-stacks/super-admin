package com.sara.superadmin.service;

import com.sara.superadmin.dto.LoginRequest;
import com.sara.superadmin.dto.LoginResponse;
import com.sara.superadmin.model.SuperAdmin;
import com.sara.superadmin.repository.SuperAdminRepository;
import com.sara.superadmin.security.JwtService;
import com.sara.superadmin.web.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthService {

	private final SuperAdminRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(SuperAdminRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public LoginResponse login(LoginRequest request) {
		SuperAdmin admin = repository.findByUsername(request.username())
				.orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));

		if (!admin.isEnabled() || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
			throw ApiException.unauthorized("Invalid username or password");
		}

		admin.setLastLoginAt(Instant.now());
		repository.save(admin);

		String token = jwtService.generateToken(admin.getUsername());
		return new LoginResponse(token, admin.getUsername(), admin.getName());
	}
}
