package com.sara.superadmin.dto;

import com.sara.superadmin.model.PaymentGateway;
import com.sara.superadmin.model.PlanDuration;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Sent by a store backend to start a subscription purchase. The price is resolved
 * server-side from the Plan matrix using duration + selectedGateways.size().
 */
public record InitiateSubscriptionRequest(
		@NotNull PlanDuration duration,
		/** Gateways the store wants to unlock; minimum 2 enforced server-side. */
		@NotNull List<PaymentGateway> selectedGateways
) {}
