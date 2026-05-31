package com.sara.superadmin.ai;

/**
 * Input to an {@link AiProvider}. {@code systemInstruction} frames the task;
 * {@code userContent} carries the (PII-stripped) incident details + any past-incident
 * context. Providers turn this into their own request shape.
 */
public record AiAnalysisPrompt(
		String systemInstruction,
		String userContent
) {}
