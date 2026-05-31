package com.sara.superadmin.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

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
		Boolean enabled,
		/** Complimentary website maintenance coverage ends at this instant (optional). */
		Instant maintenanceFreeUntil,
		/** Subscription service codes this store offers, e.g. ["PAYMENT","MAINTENANCE"]. */
		List<String> enabledServices
) {}
