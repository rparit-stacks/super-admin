package com.sara.superadmin.ai;

import com.sara.superadmin.service.AiProviderConfigService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Picks the active {@link AiProvider} from {@code ai_provider_config.activeProvider}.
 * Spring injects all provider beans; we match by {@link AiProvider#code()}.
 */
@Component
public class AiProviderSelector {

	private final List<AiProvider> providers;
	private final AiProviderConfigService configService;

	public AiProviderSelector(List<AiProvider> providers, AiProviderConfigService configService) {
		this.providers = providers;
		this.configService = configService;
	}

	/** The configured active provider, if it exists and is enabled+configured. */
	public Optional<AiProvider> active() {
		String code = configService.getOrEmpty().getActiveProvider();
		if (code == null || code.isBlank()) {
			return Optional.empty();
		}
		String wanted = code.toUpperCase();
		return providers.stream()
				.filter(p -> p.code().equals(wanted))
				.filter(AiProvider::isEnabled)
				.findFirst();
	}
}
