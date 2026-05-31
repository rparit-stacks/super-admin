package com.sara.superadmin.dto;

import java.time.Instant;

/**
 * Set or clear a store's complimentary maintenance window. A {@code null} value
 * explicitly clears it. This is the only payload that mutates {@code maintenanceFreeUntil},
 * so a general store edit can never wipe it as a side effect.
 */
public record MaintenanceWindowRequest(
		Instant maintenanceFreeUntil
) {}
