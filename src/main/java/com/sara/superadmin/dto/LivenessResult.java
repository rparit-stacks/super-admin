package com.sara.superadmin.dto;

import com.sara.superadmin.model.StoreStatus;

import java.time.Instant;

/** Result of pinging a store's health endpoint. */
public record LivenessResult(
		String storeId,
		StoreStatus status,
		Long latencyMs,
		Integer httpStatus,
		Instant checkedAt,
		String detail
) {}
