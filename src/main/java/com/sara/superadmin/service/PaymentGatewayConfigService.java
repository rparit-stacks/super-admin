package com.sara.superadmin.service;

import com.sara.superadmin.dto.PaymentGatewayConfigDto;
import com.sara.superadmin.model.PaymentGatewayConfig;
import com.sara.superadmin.repository.PaymentGatewayConfigRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Owns the singleton {@link PaymentGatewayConfig} document and the combined
 * read/write for the super-admin Payment Gateway screen. The provider-specific
 * services ({@link RazorpayPaymentService}, {@link CashfreePaymentService}) read
 * this config but do not manage it.
 */
@Service
public class PaymentGatewayConfigService {

	private final PaymentGatewayConfigRepository configRepository;

	public PaymentGatewayConfigService(PaymentGatewayConfigRepository configRepository) {
		this.configRepository = configRepository;
	}

	/** The singleton config, or an empty (all-disabled) one if none exists yet. */
	public PaymentGatewayConfig getOrEmpty() {
		return configRepository.findFirstByOrderByIdAsc()
				.orElse(PaymentGatewayConfig.builder().build());
	}

	public PaymentGatewayConfigDto getConfigMasked() {
		PaymentGatewayConfig cfg = getOrEmpty();
		return new PaymentGatewayConfigDto(
				cfg.getRazorpayKeyId() == null ? "" : cfg.getRazorpayKeyId(),
				maskSecret(cfg.getRazorpayKeySecret()),
				cfg.isRazorpayEnabled(),
				cfg.getCashfreeAppId() == null ? "" : cfg.getCashfreeAppId(),
				maskSecret(cfg.getCashfreeSecretKey()),
				cfg.isCashfreeEnabled(),
				cfg.getCashfreeEnv() == null ? "PRODUCTION" : cfg.getCashfreeEnv());
	}

	public PaymentGatewayConfigDto updateConfig(PaymentGatewayConfigDto dto) {
		PaymentGatewayConfig cfg = configRepository.findFirstByOrderByIdAsc()
				.orElseGet(() -> PaymentGatewayConfig.builder().build());

		// ---- Razorpay ----
		cfg.setRazorpayKeyId(dto.razorpayKeyId());
		if (isProvidedSecret(dto.razorpayKeySecret())) {
			cfg.setRazorpayKeySecret(dto.razorpayKeySecret());
		}
		cfg.setRazorpayEnabled(dto.razorpayEnabled());

		// ---- Cashfree ----
		cfg.setCashfreeAppId(dto.cashfreeAppId());
		if (isProvidedSecret(dto.cashfreeSecretKey())) {
			cfg.setCashfreeSecretKey(dto.cashfreeSecretKey());
		}
		cfg.setCashfreeEnabled(dto.cashfreeEnabled());
		cfg.setCashfreeEnv(normalizeEnv(dto.cashfreeEnv()));

		cfg.setUpdatedAt(Instant.now());
		configRepository.save(cfg);
		return getConfigMasked();
	}

	private static String maskSecret(String secret) {
		return (secret != null && !secret.isBlank()) ? PaymentGatewayConfigDto.SECRET_MASK : "";
	}

	/** Empty or the masked placeholder means "keep existing". */
	private static boolean isProvidedSecret(String secret) {
		return secret != null
				&& !secret.isBlank()
				&& !PaymentGatewayConfigDto.SECRET_MASK.equals(secret);
	}

	private static String normalizeEnv(String env) {
		if (env != null && "SANDBOX".equalsIgnoreCase(env)) {
			return "SANDBOX";
		}
		return "PRODUCTION";
	}
}
