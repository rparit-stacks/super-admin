package com.sara.superadmin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payment-confirmation payload, sent by the store to confirm payment. Provider-aware:
 * for Razorpay the order/payment/signature triplet is required; for Cashfree only the
 * Cashfree order id (verification is a server-side fetch, no client signature). The
 * provider-specific fields are validated in the service, not here.
 */
public record VerifySubscriptionRequest(
		@NotBlank String subscriptionId,
		/** RAZORPAY (default if null) or CASHFREE. */
		String paymentProvider,
		// Razorpay
		String razorpayOrderId,
		String razorpayPaymentId,
		String razorpaySignature,
		// Cashfree
		String cashfreeOrderId
) {}
