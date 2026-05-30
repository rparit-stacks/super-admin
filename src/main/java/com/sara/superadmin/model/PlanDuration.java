package com.sara.superadmin.model;

/**
 * Subscription plan durations. LIFETIME means no expiry (endDate stays null).
 */
public enum PlanDuration {
	SIX_MONTH(6),
	ONE_YEAR(12),
	LIFETIME(0);

	private final int months;

	PlanDuration(int months) {
		this.months = months;
	}

	/** Number of months this duration adds; 0 for LIFETIME (never expires). */
	public int getMonths() {
		return months;
	}

	public boolean isLifetime() {
		return this == LIFETIME;
	}
}
