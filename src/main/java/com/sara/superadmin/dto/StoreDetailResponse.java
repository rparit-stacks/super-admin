package com.sara.superadmin.dto;

import java.util.List;

/**
 * "Go into a store" view for the super-admin: the store itself, its current
 * liveness, the active subscription (if any) and the full subscription history.
 */
public record StoreDetailResponse(
		StoreResponse store,
		SubscriptionDto activeSubscription,
		List<SubscriptionDto> subscriptionHistory
) {}
