package com.sara.superadmin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * A store's subscription purchase. One store can have many over time (history);
 * at most one is ACTIVE at a given moment. The selectedGateways list is what the
 * store backend uses to decide which payment gateways are unlocked.
 */
@Document(collection = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

	@Id
	private String id;

	/** Which store this belongs to. */
	@Indexed
	private String storeId;

	private PlanDuration duration;

	/** How many gateways were purchased (== selectedGateways.size(), min 2). */
	private int serviceCount;

	/** The gateways the store unlocked with this subscription. */
	private List<PaymentGateway> selectedGateways;

	/** Amount charged, resolved from the Plan matrix at purchase time. */
	private BigDecimal amount;

	@Builder.Default
	private String currency = "INR";

	@Builder.Default
	private SubscriptionStatus status = SubscriptionStatus.PENDING;

	/** Set when payment is verified. */
	private Instant startDate;

	/** Null for LIFETIME plans; otherwise startDate + duration. */
	private Instant endDate;

	// ----- Razorpay payment linkage -----
	private String razorpayOrderId;
	private String razorpayPaymentId;
	private String razorpaySignature;

	private Instant createdAt;
	private Instant updatedAt;
}
