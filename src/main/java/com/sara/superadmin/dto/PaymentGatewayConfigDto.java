package com.sara.superadmin.dto;

/**
 * Super-admin payment-gateway config for both providers (Razorpay + Cashfree).
 * On read, secrets are masked. On write, an empty/masked secret means "leave unchanged".
 */
public record PaymentGatewayConfigDto(
		// Razorpay
		String razorpayKeyId,
		String razorpayKeySecret,
		boolean razorpayEnabled,
		// Cashfree
		String cashfreeAppId,
		String cashfreeSecretKey,
		boolean cashfreeEnabled,
		String cashfreeEnv
) {
	public static final String SECRET_MASK = "***SECRET_SET***";
}
