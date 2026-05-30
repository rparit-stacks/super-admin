package com.sara.superadmin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A super-admin login account. BCrypt-hashed password.
 */
@Document(collection = "super_admins")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdmin {

	@Id
	private String id;

	@Indexed(unique = true)
	private String username;

	/** BCrypt hash. */
	private String passwordHash;

	private String name;

	@Builder.Default
	private boolean enabled = true;

	private Instant createdAt;
	private Instant lastLoginAt;
}
