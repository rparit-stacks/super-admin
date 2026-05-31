package com.sara.superadmin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A raw error signal pushed by a store's RCA plugin. One per store api_log error row.
 * Deduped within a window by {@code errorSignature} (occurrenceCount bumped instead of
 * inserting duplicates). The unique (storeId, storeApiLogId) guard makes ingest idempotent.
 */
@Document(collection = "incidents")
@CompoundIndex(name = "store_apilog_uq", def = "{'storeId': 1, 'storeApiLogId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

	@Id
	private String id;

	@Indexed
	private String storeId;
	private String storeName;

	private String apiEndpoint;
	private String httpMethod;
	private Integer statusCode;
	private String errorFlag;          // RED | ORANGE
	private String errorMessage;       // PII-stripped
	private String stackTrace;         // PII-stripped, truncated
	private Long responseTimeMs;
	private Instant occurredAt;        // store-side api_log.timestamp

	@Indexed
	private String errorSignature;     // normalized: endpoint + status + msg-head
	@Builder.Default
	private int occurrenceCount = 1;

	/** NEW | ANALYZED | NOTIFIED | IGNORED */
	@Indexed
	@Builder.Default
	private String status = "NEW";

	/** Source api_log.id on the store, for traceability + idempotency. */
	private Long storeApiLogId;

	private Instant createdAt;
	private Instant updatedAt;
}
