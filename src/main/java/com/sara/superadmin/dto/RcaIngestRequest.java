package com.sara.superadmin.dto;

import java.util.List;

/**
 * Batch of error rows pushed by a store's RCA plugin to /api/store-api/rca/ingest.
 * The storeId is resolved from the X-Store-Api-Key (not trusted from the body).
 */
public record RcaIngestRequest(
		List<Row> rows
) {
	public record Row(
			Long apiLogId,
			String apiEndpoint,
			String httpMethod,
			Integer statusCode,
			String errorFlag,
			String errorMessage,
			String stackTrace,
			Long responseTimeMs,
			String occurredAt   // ISO-8601 instant string
	) {}
}
