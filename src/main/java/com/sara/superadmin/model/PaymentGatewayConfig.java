package com.sara.superadmin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * Super-admin's OWN payment-gateway credentials, used to collect subscription
 * payments from store admins. Singleton (single document). Store admins never see
 * the secrets; only the public identifiers are exposed to the checkout.
 *
 * <p>Supports two providers, each independently toggleable: Razorpay and Cashfree.
 */
@Document(collection = "payment_gateway_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayConfig {

	@Id
	private String id;

	// ---------------- Razorpay ----------------

	private String razorpayKeyId;

	/** Stored as-is; never returned to clients. */
	private String razorpayKeySecret;

	/**
	 * Whether Razorpay is enabled. Persisted under the legacy field name {@code enabled}
	 * so existing documents keep working without a migration.
	 */
	@Builder.Default
	@Field("enabled")
	private boolean razorpayEnabled = false;

	// ---------------- Cashfree ----------------

	private String cashfreeAppId;

	/** Stored as-is; never returned to clients. */
	private String cashfreeSecretKey;

	@Builder.Default
	private boolean cashfreeEnabled = false;

	/** {@code "SANDBOX"} or {@code "PRODUCTION"}. */
	@Builder.Default
	private String cashfreeEnv = "PRODUCTION";

	private Instant updatedAt;
}
