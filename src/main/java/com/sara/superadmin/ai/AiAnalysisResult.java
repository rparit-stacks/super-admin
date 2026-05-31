package com.sara.superadmin.ai;

/**
 * Parsed output of an AI analysis. {@code rootCause} and {@code suggestedFix} are the
 * human-facing fields shown in the alert email and dashboard. {@code rawResponse} is
 * kept for debugging. {@code confidence} is best-effort (HIGH|MEDIUM|LOW), may be null.
 */
public record AiAnalysisResult(
		String rootCause,
		String suggestedFix,
		String confidence,
		String rawResponse,
		String modelUsed,
		long latencyMs
) {}
