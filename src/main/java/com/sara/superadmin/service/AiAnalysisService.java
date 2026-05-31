package com.sara.superadmin.service;

import com.sara.superadmin.ai.AiAnalysisPrompt;
import com.sara.superadmin.ai.AiAnalysisResult;
import com.sara.superadmin.ai.AiProvider;
import com.sara.superadmin.ai.AiProviderSelector;
import com.sara.superadmin.model.Incident;
import com.sara.superadmin.model.IncidentAnalysis;
import com.sara.superadmin.repository.IncidentAnalysisRepository;
import com.sara.superadmin.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The "brain": takes a NEW incident, builds a prompt (incident + similar past fixes),
 * runs the active AI provider, persists the analysis, emails the alert, and marks the
 * incident ANALYZED. No active/enabled provider → incident is left NEW for a later sweep.
 */
@Service
public class AiAnalysisService {

	private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

	private static final String SYSTEM_INSTRUCTION = """
			You are a senior backend SRE analyzing a production error from an e-commerce store.
			Given the error details and any past similar incidents, respond in EXACTLY this format:
			ROOT CAUSE: <one concise paragraph on the most probable cause>
			SUGGESTED FIX: <concrete, actionable steps a developer can take>
			CONFIDENCE: <HIGH | MEDIUM | LOW>
			Be specific and technical. Do not invent data not present in the input.""";

	private final IncidentRepository incidentRepository;
	private final IncidentAnalysisRepository analysisRepository;
	private final AiProviderSelector selector;
	private final RcaAlertEmailService emailService;

	public AiAnalysisService(IncidentRepository incidentRepository,
							 IncidentAnalysisRepository analysisRepository,
							 AiProviderSelector selector,
							 RcaAlertEmailService emailService) {
		this.incidentRepository = incidentRepository;
		this.analysisRepository = analysisRepository;
		this.selector = selector;
		this.emailService = emailService;
	}

	/** Analyze up to {@code max} NEW incidents. Returns how many were analyzed. */
	public int analyzeNewIncidents(int max) {
		AiProvider provider = selector.active().orElse(null);
		if (provider == null) {
			log.debug("RCA sweep skipped: no active/enabled AI provider configured");
			return 0;
		}
		List<Incident> batch = incidentRepository.findByStatusOrderByCreatedAtAsc("NEW", PageRequest.of(0, max));
		int done = 0;
		for (Incident incident : batch) {
			try {
				analyzeOne(incident, provider);
				done++;
			} catch (Exception e) {
				log.error("RCA analysis failed for incident {}: {}", incident.getId(), e.getMessage());
				// Leave as NEW so a later sweep can retry.
			}
		}
		return done;
	}

	/** Analyze a single incident immediately (used by manual re-analyze too). */
	public IncidentAnalysis analyzeOne(Incident incident, AiProvider provider) {
		List<IncidentAnalysis> past = analysisRepository
				.findByErrorSignatureOrderByCreatedAtDesc(incident.getErrorSignature(), PageRequest.of(0, 3));

		AiAnalysisResult result = provider.analyze(
				new AiAnalysisPrompt(SYSTEM_INSTRUCTION, buildUserContent(incident, past)));

		List<String> pastIds = new ArrayList<>();
		for (IncidentAnalysis p : past) {
			pastIds.add(p.getIncidentId());
		}

		IncidentAnalysis analysis = IncidentAnalysis.builder()
				.incidentId(incident.getId())
				.storeId(incident.getStoreId())
				.errorSignature(incident.getErrorSignature())
				.providerUsed(provider.code())
				.modelUsed(result.modelUsed())
				.rootCause(result.rootCause())
				.suggestedFix(result.suggestedFix())
				.confidence(result.confidence())
				.similarPastIncidentIds(pastIds)
				.latencyMs(result.latencyMs())
				.createdAt(Instant.now())
				.build();
		analysis = analysisRepository.save(analysis);

		boolean sent = emailService.sendIncidentAlert(incident, analysis);
		if (sent) {
			analysis.setEmailSent(true);
			analysis.setEmailSentAt(Instant.now());
			analysisRepository.save(analysis);
		}

		incident.setStatus(sent ? "NOTIFIED" : "ANALYZED");
		incident.setUpdatedAt(Instant.now());
		incidentRepository.save(incident);
		return analysis;
	}

	private static String buildUserContent(Incident i, List<IncidentAnalysis> past) {
		StringBuilder sb = new StringBuilder();
		sb.append("CURRENT ERROR\n");
		sb.append("Store: ").append(i.getStoreName()).append('\n');
		sb.append("Endpoint: ").append(i.getHttpMethod()).append(' ').append(i.getApiEndpoint()).append('\n');
		sb.append("Status: ").append(i.getStatusCode()).append(" (").append(i.getErrorFlag()).append(")\n");
		sb.append("Latency: ").append(i.getResponseTimeMs()).append(" ms\n");
		sb.append("Message: ").append(i.getErrorMessage()).append('\n');
		if (i.getStackTrace() != null && !i.getStackTrace().isBlank()) {
			sb.append("Stack trace:\n").append(i.getStackTrace()).append('\n');
		}
		if (past != null && !past.isEmpty()) {
			sb.append("\nPAST SIMILAR INCIDENTS (most recent first) — reuse working fixes when relevant:\n");
			for (IncidentAnalysis p : past) {
				sb.append("- Root cause: ").append(p.getRootCause()).append('\n');
				sb.append("  Fix that was suggested: ").append(p.getSuggestedFix()).append('\n');
			}
		}
		return sb.toString();
	}
}
