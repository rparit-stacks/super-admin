package com.sara.superadmin.dto;

/**
 * Super-admin Razorpay config. On read, the secret is masked. On write, an empty
 * secret means "leave unchanged".
 */
public record PaymentGatewayConfigDto(
		String razorpayKeyId,
		String razorpayKeySecret,
		boolean enabled
) {
	public static final String SECRET_MASK = "***SECRET_SET***";
}
