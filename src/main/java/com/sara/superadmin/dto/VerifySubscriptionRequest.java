package com.sara.superadmin.dto;

import jakarta.validation.constraints.NotBlank;

/** Razorpay checkout result, sent by the store to confirm payment. */
public record VerifySubscriptionRequest(
		@NotBlank String subscriptionId,
		@NotBlank String razorpayOrderId,
		@NotBlank String razorpayPaymentId,
		@NotBlank String razorpaySignature
) {}
