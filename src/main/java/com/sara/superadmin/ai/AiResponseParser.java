package com.sara.superadmin.ai;

/**
 * Turns raw model text into a structured {@link AiAnalysisResult}. We instruct models
 * to answer in a simple labelled format:
 *   ROOT CAUSE: ...
 *   SUGGESTED FIX: ...
 *   CONFIDENCE: HIGH|MEDIUM|LOW
 * and parse leniently (fall back to the whole text as root cause if labels are absent).
 */
final class AiResponseParser {

	private AiResponseParser() {}

	static AiAnalysisResult toResult(String text, String rawResponse, String model, long latencyMs) {
		String safe = text == null ? "" : text.trim();
		String rootCause = section(safe, "ROOT CAUSE");
		String fix = section(safe, "SUGGESTED FIX");
		String confidence = section(safe, "CONFIDENCE");

		if (rootCause.isBlank() && fix.isBlank()) {
			// Model didn't follow the format — keep the whole answer as the root cause.
			rootCause = safe.isBlank() ? "No analysis returned." : safe;
		}
		return new AiAnalysisResult(
				rootCause.isBlank() ? "Unknown" : rootCause,
				fix.isBlank() ? "No specific fix suggested." : fix,
				normalizeConfidence(confidence),
				rawResponse,
				model,
				latencyMs);
	}

	/** Extract text after "LABEL:" up to the next known label or end. Case-insensitive. */
	private static String section(String text, String label) {
		String lower = text.toLowerCase();
		String key = label.toLowerCase() + ":";
		int start = lower.indexOf(key);
		if (start < 0) {
			return "";
		}
		start += key.length();
		int end = text.length();
		for (String other : new String[]{"root cause:", "suggested fix:", "confidence:"}) {
			if (other.equals(key)) {
				continue;
			}
			int idx = lower.indexOf(other, start);
			if (idx >= 0 && idx < end) {
				end = idx;
			}
		}
		return text.substring(start, end).trim();
	}

	private static String normalizeConfidence(String c) {
		if (c == null) {
			return null;
		}
		String up = c.trim().toUpperCase();
		if (up.startsWith("HIGH")) return "HIGH";
		if (up.startsWith("MED")) return "MEDIUM";
		if (up.startsWith("LOW")) return "LOW";
		return null;
	}
}
