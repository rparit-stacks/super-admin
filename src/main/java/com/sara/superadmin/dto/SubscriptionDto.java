package com.sara.superadmin.dto;

import com.sara.superadmin.model.PaymentGateway;
import com.sara.superadmin.model.PlanDuration;
import com.sara.superadmin.model.Subscription;
import com.sara.superadmin.model.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SubscriptionDto(
		String id,
		String storeId,
		PlanDuration duration,
		int serviceCount,
		List<PaymentGateway> selectedGateways,
		BigDecimal amount,
		String currency,
		/** {@code PAYMENT} or {@code MAINTENANCE}. */
		String productLine,
		SubscriptionStatus status,
		Instant startDate,
		Instant endDate,
		String razorpayOrderId,
		String razorpayPaymentId,
		Instant createdAt
) {
	public static SubscriptionDto from(Subscription s) {
		return new SubscriptionDto(
				s.getId(), s.getStoreId(), s.getDuration(), s.getServiceCount(),
				s.getSelectedGateways(), s.getAmount(), s.getCurrency(), s.getProductLine(),
				s.getStatus(),
				s.getStartDate(), s.getEndDate(), s.getRazorpayOrderId(),
				s.getRazorpayPaymentId(), s.getCreatedAt());
	}
}
