package com.sara.superadmin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Super-admin's AI/LLM provider credentials for the Root Cause Analyzer. Singleton
 * document. API keys are stored as-is but never returned to clients (masked), exactly
 * like {@link PaymentGatewayConfig}.
 */
@Document(collection = "ai_provider_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProviderConfig {

	@Id
	private String id;

	/** Which provider is active: GEMINI | OPENAI | ANTHROPIC | PERPLEXITY. */
	@Builder.Default
	private String activeProvider = "GEMINI";

	// ---- Gemini ----
	private String geminiApiKey;
	@Builder.Default
	private String geminiModel = "gemini-2.0-flash";
	@Builder.Default
	private boolean geminiEnabled = false;

	// ---- OpenAI ----
	private String openAiApiKey;
	@Builder.Default
	private String openAiModel = "gpt-4o-mini";
	@Builder.Default
	private boolean openAiEnabled = false;

	// ---- Anthropic ----
	private String anthropicApiKey;
	@Builder.Default
	private String anthropicModel = "claude-3-5-haiku-latest";
	@Builder.Default
	private boolean anthropicEnabled = false;

	// ---- Perplexity ----
	private String perplexityApiKey;
	@Builder.Default
	private String perplexityModel = "sonar";
	@Builder.Default
	private boolean perplexityEnabled = false;

	// ---- Alerting (SMTP + recipients), editable from UI; falls back to properties ----
	/** Comma-separated recipient emails for RCA alerts. */
	private String alertToEmails;
	private String smtpHost;
	private Integer smtpPort;
	private String smtpUsername;
	/** Stored as-is, never returned. */
	private String smtpPassword;
	private String smtpFrom;
	@Builder.Default
	private boolean smtpSslEnabled = true;

	private Instant updatedAt;
}
