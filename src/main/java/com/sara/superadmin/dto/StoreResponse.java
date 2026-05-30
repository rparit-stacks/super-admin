package com.sara.superadmin.dto;

import com.sara.superadmin.model.Store;
import com.sara.superadmin.model.StoreStatus;

import java.time.Instant;

/**
 * Store as returned to the super-admin UI. Includes the apiKey (super-admin is
 * trusted) plus current liveness fields.
 */
public record StoreResponse(
		String id,
		String name,
		String code,
		String apiBaseUrl,
		String websiteUrl,
		String apiKey,
		StoreStatus status,
		Instant lastPingAt,
		Long lastPingLatencyMs,
		boolean enabled,
		String contactEmail,
		String contactPhone,
		String notes,
		Instant maintenanceFreeUntil,
		Instant createdAt,
		Instant updatedAt
) {
	public static StoreResponse from(Store s) {
		return new StoreResponse(
				s.getId(), s.getName(), s.getCode(), s.getApiBaseUrl(), s.getWebsiteUrl(),
				s.getApiKey(), s.getStatus(), s.getLastPingAt(), s.getLastPingLatencyMs(),
				s.isEnabled(), s.getContactEmail(), s.getContactPhone(), s.getNotes(),
				s.getMaintenanceFreeUntil(),
				s.getCreatedAt(), s.getUpdatedAt());
	}
}
