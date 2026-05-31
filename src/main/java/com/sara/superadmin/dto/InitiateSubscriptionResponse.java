package com.sara.superadmin.dto;

import java.math.BigDecimal;

/**
 * Returned to the store after initiating. Carries everything the store frontend
 * needs to open the chosen provider's checkout: for Razorpay the order id + public
 * key, for Cashfree the payment session id + order id + env. When {@code free} is
 * true the subscription was activated with no payment (zero price plan).
 */
public record InitiateSubscriptionResponse(
		String subscriptionId,
		/** RAZORPAY or CASHFREE. */
		String paymentProvider,
		// Razorpay
		String razorpayOrderId,
		String razorpayKeyId,
		// Cashfree
		String cashfreeOrderId,
		String cashfreePaymentSessionId,
		String cashfreeEnv,
		BigDecimal amount,
		String currency,
		int serviceCount,
		boolean free
) {
	/** Free-activation response (no payment, no provider needed). */
	public static InitiateSubscriptionResponse free(String subscriptionId, String currency, int serviceCount) {
		return new InitiateSubscriptionResponse(
				subscriptionId, null, null, null, null, null, null,
				BigDecimal.ZERO, currency, serviceCount, true);
	}

	/** Razorpay checkout response. */
	public static InitiateSubscriptionResponse razorpay(String subscriptionId, String orderId, String keyId,
														BigDecimal amount, String currency, int serviceCount) {
		return new InitiateSubscriptionResponse(
				subscriptionId, "RAZORPAY", orderId, keyId, null, null, null,
				amount, currency, serviceCount, false);
	}

	/** Cashfree checkout response. */
	public static InitiateSubscriptionResponse cashfree(String subscriptionId, String cfOrderId, String sessionId,
														String env, BigDecimal amount, String currency, int serviceCount) {
		return new InitiateSubscriptionResponse(
				subscriptionId, "CASHFREE", null, null, cfOrderId, sessionId, env,
				amount, currency, serviceCount, false);
	}
}
