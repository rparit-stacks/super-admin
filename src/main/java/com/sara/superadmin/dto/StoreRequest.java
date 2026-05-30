package com.sara.superadmin.dto;

import jakarta.validation.constraints.NotBlank;

/** Create / update a store. apiKey is generated server-side when omitted. */
public record StoreRequest(
		@NotBlank String name,
		String code,
		@NotBlank String apiBaseUrl,
		String websiteUrl,
		String apiKey,
		String contactEmail,
		String contactPhone,
		String notes,
		Boolean enabled
) {}
