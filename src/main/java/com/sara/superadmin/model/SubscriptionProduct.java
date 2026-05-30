package com.sara.superadmin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A subscription offering shown on the store admin "Subscription" picker.
 * Super-admin controls the list; {@link #checkoutFlow} selects which purchase UI the store app runs.
 */
@Document(collection = "subscription_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionProduct {

	@Id
	private String id;

	/** Stable machine id, e.g. PAYMENT, CHAT_AI */
	@Indexed(unique = true)
	private String code;

	private String title;

	private String description;

	@Builder.Default
	private boolean visibleToStores = true;

	@Builder.Default
	private int sortOrder = 0;

	/**
	 * For {@code MAINTENANCE_MONTHLY} checkout: price per month (INR). Ignored for payment subscription.
	 */
	private java.math.BigDecimal monthlyPrice;

	/**
	 * Storefront checkout implementation key, e.g. {@code PAYMENT_GATEWAYS}.
	 * When null, the product is listed but not purchasable yet (coming soon).
	 */
	private String checkoutFlow;
}
