package com.sara.superadmin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/** Builds and validates super-admin JWTs. */
@Service
public class JwtService {

	@Value("${super-admin.jwt.secret}")
	private String secret;

	@Value("${super-admin.jwt.expiration-ms}")
	private long expirationMs;

	private SecretKey signingKey() {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public String generateToken(String username) {
		long now = System.currentTimeMillis();
		return Jwts.builder()
				.claims(Map.of("type", "super-admin"))
				.subject(username)
				.issuedAt(new Date(now))
				.expiration(new Date(now + expirationMs))
				.signWith(signingKey())
				.compact();
	}

	/** Returns the username if the token is valid and unexpired, else null. */
	public String validateAndGetUsername(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(signingKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();
			if (!"super-admin".equals(claims.get("type"))) {
				return null;
			}
			if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
				return null;
			}
			return claims.getSubject();
		} catch (JwtException | IllegalArgumentException e) {
			return null;
		}
	}
}
