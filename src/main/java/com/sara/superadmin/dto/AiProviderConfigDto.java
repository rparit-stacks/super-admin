package com.sara.superadmin.dto;

/**
 * AI provider config for the super-admin RCA screen. On read, API keys are masked.
 * On write, an empty/masked key means "leave unchanged" (same as PaymentGatewayConfigDto).
 */
public record AiProviderConfigDto(
		String activeProvider,
		String geminiApiKey,
		String geminiModel,
		boolean geminiEnabled,
		String openAiApiKey,
		String openAiModel,
		boolean openAiEnabled,
		String anthropicApiKey,
		String anthropicModel,
		boolean anthropicEnabled,
		String perplexityApiKey,
		String perplexityModel,
		boolean perplexityEnabled,
		// Alerting
		String alertToEmails,
		String smtpHost,
		Integer smtpPort,
		String smtpUsername,
		String smtpPassword,
		String smtpFrom,
		boolean smtpSslEnabled
) {
	public static final String SECRET_MASK = "***SECRET_SET***";
}
