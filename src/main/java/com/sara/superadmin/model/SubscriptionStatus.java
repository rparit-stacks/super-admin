package com.sara.superadmin.model;

public enum SubscriptionStatus {
	/** Order created, payment not yet verified. */
	PENDING,
	/** Payment verified, subscription is live. */
	ACTIVE,
	/** endDate has passed (non-lifetime plans only). */
	EXPIRED,
	/** Payment failed or order abandoned. */
	FAILED
}
