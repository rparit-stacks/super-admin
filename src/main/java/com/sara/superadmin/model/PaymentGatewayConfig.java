package com.sara.superadmin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Super-admin's OWN Razorpay credentials, used to collect subscription payments
 * from store admins. Singleton (single document). Store admins never see these;
 * only the public keyId is exposed to the checkout.
 */
@Document(collection = "payment_gateway_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGatewayConfig {

	@Id
	private String id;

	private String razorpayKeyId;

	/** Stored as-is; never returned to clients. */
	private String razorpayKeySecret;

	@Builder.Default
	private boolean enabled = false;

	private Instant updatedAt;
}
