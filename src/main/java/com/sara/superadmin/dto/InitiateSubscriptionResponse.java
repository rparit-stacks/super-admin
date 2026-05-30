package com.sara.superadmin.dto;

import java.math.BigDecimal;

/**
 * Returned to the store after initiating. Carries everything the store frontend
 * needs to open the Razorpay checkout (using super-admin's public key).
 */
public record InitiateSubscriptionResponse(
		String subscriptionId,
		String razorpayOrderId,
		String razorpayKeyId,
		BigDecimal amount,
		String currency,
		int serviceCount
) {}
