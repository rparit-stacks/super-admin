package com.sara.superadmin.service;

import com.sara.superadmin.dto.AiProviderConfigDto;
import com.sara.superadmin.model.AiProviderConfig;
import com.sara.superadmin.repository.AiProviderConfigRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Owns the singleton {@link AiProviderConfig} document and the masked read/write for
 * the super-admin RCA settings. Mirrors {@link PaymentGatewayConfigService}: API keys
 * are never returned; an empty/masked key on write means "keep existing".
 */
@Service
public class AiProviderConfigService {

	private final AiProviderConfigRepository repository;

	public AiProviderConfigService(AiProviderConfigRepository repository) {
		this.repository = repository;
	}

	public AiProviderConfig getOrEmpty() {
		return repository.findFirstByOrderByIdAsc()
				.orElse(AiProviderConfig.builder().build());
	}

	public AiProviderConfigDto getConfigMasked() {
		AiProviderConfig c = getOrEmpty();
		return new AiProviderConfigDto(
				c.getActiveProvider() == null ? "GEMINI" : c.getActiveProvider(),
				mask(c.getGeminiApiKey()), nz(c.getGeminiModel()), c.isGeminiEnabled(),
				mask(c.getOpenAiApiKey()), nz(c.getOpenAiModel()), c.isOpenAiEnabled(),
				mask(c.getAnthropicApiKey()), nz(c.getAnthropicModel()), c.isAnthropicEnabled(),
				mask(c.getPerplexityApiKey()), nz(c.getPerplexityModel()), c.isPerplexityEnabled(),
				nz(c.getAlertToEmails()),
				nz(c.getSmtpHost()),
				c.getSmtpPort(),
				nz(c.getSmtpUsername()),
				mask(c.getSmtpPassword()),
				nz(c.getSmtpFrom()),
				c.isSmtpSslEnabled());
	}

	public AiProviderConfigDto updateConfig(AiProviderConfigDto dto) {
		AiProviderConfig c = repository.findFirstByOrderByIdAsc()
				.orElseGet(() -> AiProviderConfig.builder().build());

		if (dto.activeProvider() != null && !dto.activeProvider().isBlank()) {
			c.setActiveProvider(dto.activeProvider().toUpperCase());
		}

		if (isProvided(dto.geminiApiKey())) c.setGeminiApiKey(dto.geminiApiKey());
		if (dto.geminiModel() != null && !dto.geminiModel().isBlank()) c.setGeminiModel(dto.geminiModel());
		c.setGeminiEnabled(dto.geminiEnabled());

		if (isProvided(dto.openAiApiKey())) c.setOpenAiApiKey(dto.openAiApiKey());
		if (dto.openAiModel() != null && !dto.openAiModel().isBlank()) c.setOpenAiModel(dto.openAiModel());
		c.setOpenAiEnabled(dto.openAiEnabled());

		if (isProvided(dto.anthropicApiKey())) c.setAnthropicApiKey(dto.anthropicApiKey());
		if (dto.anthropicModel() != null && !dto.anthropicModel().isBlank()) c.setAnthropicModel(dto.anthropicModel());
		c.setAnthropicEnabled(dto.anthropicEnabled());

		if (isProvided(dto.perplexityApiKey())) c.setPerplexityApiKey(dto.perplexityApiKey());
		if (dto.perplexityModel() != null && !dto.perplexityModel().isBlank()) c.setPerplexityModel(dto.perplexityModel());
		c.setPerplexityEnabled(dto.perplexityEnabled());

		// Alerting
		if (dto.alertToEmails() != null) c.setAlertToEmails(dto.alertToEmails());
		if (dto.smtpHost() != null) c.setSmtpHost(dto.smtpHost());
		if (dto.smtpPort() != null) c.setSmtpPort(dto.smtpPort());
		if (dto.smtpUsername() != null) c.setSmtpUsername(dto.smtpUsername());
		if (isProvided(dto.smtpPassword())) c.setSmtpPassword(dto.smtpPassword());
		if (dto.smtpFrom() != null) c.setSmtpFrom(dto.smtpFrom());
		c.setSmtpSslEnabled(dto.smtpSslEnabled());

		c.setUpdatedAt(Instant.now());
		repository.save(c);
		return getConfigMasked();
	}

	private static String mask(String secret) {
		return (secret != null && !secret.isBlank()) ? AiProviderConfigDto.SECRET_MASK : "";
	}

	private static boolean isProvided(String secret) {
		return secret != null && !secret.isBlank() && !AiProviderConfigDto.SECRET_MASK.equals(secret);
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
