package com.sara.superadmin.service;

import com.sara.superadmin.dto.RcaIngestRequest;
import com.sara.superadmin.model.Incident;
import com.sara.superadmin.model.Store;
import com.sara.superadmin.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Receives error batches from store RCA plugins, scrubs PII, computes an error
 * signature, dedupes, and persists {@link Incident}s with status NEW. Idempotent on
 * (storeId, storeApiLogId).
 */
@Service
public class IncidentIngestService {

	private static final Logger log = LoggerFactory.getLogger(IncidentIngestService.class);
	private static final int MSG_MAX = 2000;
	private static final int STACK_MAX = 6000;

	private final IncidentRepository incidentRepository;

	public IncidentIngestService(IncidentRepository incidentRepository) {
		this.incidentRepository = incidentRepository;
	}

	/** Returns how many new incidents were stored (duplicates skipped). */
	public int ingest(Store store, RcaIngestRequest request) {
		if (request == null || request.rows() == null || request.rows().isEmpty()) {
			return 0;
		}
		int stored = 0;
		for (RcaIngestRequest.Row row : request.rows()) {
			if (row == null || row.apiLogId() == null) {
				continue;
			}
			// Idempotency: skip if this store+apiLogId already exists.
			if (incidentRepository.findByStoreIdAndStoreApiLogId(store.getId(), row.apiLogId()).isPresent()) {
				continue;
			}

			String msg = PiiScrubber.truncate(PiiScrubber.scrub(row.errorMessage()), MSG_MAX);
			String stack = PiiScrubber.truncate(PiiScrubber.scrub(row.stackTrace()), STACK_MAX);
			Instant now = Instant.now();

			Incident incident = Incident.builder()
					.storeId(store.getId())
					.storeName(store.getName())
					.apiEndpoint(row.apiEndpoint())
					.httpMethod(row.httpMethod())
					.statusCode(row.statusCode())
					.errorFlag(row.errorFlag())
					.errorMessage(msg)
					.stackTrace(stack)
					.responseTimeMs(row.responseTimeMs())
					.occurredAt(parseInstant(row.occurredAt()))
					.errorSignature(signature(row))
					.status("NEW")
					.storeApiLogId(row.apiLogId())
					.createdAt(now)
					.updatedAt(now)
					.build();
			try {
				incidentRepository.save(incident);
				stored++;
			} catch (DuplicateKeyException dup) {
				// Concurrent push of the same row — safe to ignore.
				log.debug("Duplicate incident skipped: store={} apiLogId={}", store.getId(), row.apiLogId());
			}
		}
		return stored;
	}

	/** Normalized signature: METHOD endpoint #status :: message-head (no digits/ids). */
	private static String signature(RcaIngestRequest.Row row) {
		String endpoint = row.apiEndpoint() == null ? "" : row.apiEndpoint()
				.replaceAll("/\\d+", "/{id}")            // collapse numeric path ids
				.replaceAll("[0-9a-fA-F]{16,}", "{hash}"); // collapse long hex ids
		String head = row.errorMessage() == null ? "" : row.errorMessage().trim();
		if (head.length() > 80) {
			head = head.substring(0, 80);
		}
		head = head.replaceAll("\\d+", "#");
		return (row.httpMethod() == null ? "" : row.httpMethod()) + " "
				+ endpoint + " #" + row.statusCode() + " :: " + head;
	}

	private static Instant parseInstant(String s) {
		if (s == null || s.isBlank()) {
			return Instant.now();
		}
		try {
			return Instant.parse(s);
		} catch (Exception e) {
			return Instant.now();
		}
	}
}
