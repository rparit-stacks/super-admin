package com.sara.superadmin.ai;

/**
 * A swappable LLM backend for the Root Cause Analyzer. One Spring bean per provider
 * (Gemini, OpenAI, Anthropic, Perplexity). {@link AiProviderSelector} picks the active
 * one from {@code ai_provider_config}.
 */
public interface AiProvider {

	/** Stable code: GEMINI | OPENAI | ANTHROPIC | PERPLEXITY. */
	String code();

	/** True when this provider is enabled AND has an API key configured. */
	boolean isEnabled();

	/** Run the analysis. Implementations call the provider's REST API via aiRestTemplate. */
	AiAnalysisResult analyze(AiAnalysisPrompt prompt);
}
