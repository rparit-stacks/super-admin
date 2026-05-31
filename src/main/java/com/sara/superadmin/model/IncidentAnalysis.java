package com.sara.superadmin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * AI analysis output for an {@link Incident}. Kept separate so re-analysis preserves
 * history. Past analyses (with a suggestedFix) feed the "use past incidents" context.
 */
@Document(collection = "incident_analyses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentAnalysis {

	@Id
	private String id;

	@Indexed
	private String incidentId;
	private String storeId;
	@Indexed
	private String errorSignature;

	private String providerUsed;       // GEMINI | OPENAI | ...
	private String modelUsed;

	private String rootCause;
	private String suggestedFix;
	private String confidence;         // HIGH | MEDIUM | LOW | null

	private List<String> similarPastIncidentIds;

	private long latencyMs;
	@Builder.Default
	private boolean emailSent = false;
	private Instant emailSentAt;

	private Instant createdAt;
}
