package com.sara.superadmin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A registered store. This is the standalone object the super-admin manages.
 * B-Nutri is the first one; more stores get added over time. Each store carries
 * its own connection details, liveness status and (via Subscription docs) its plans.
 */
@Document(collection = "stores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store {

	@Id
	private String id;

	/** Display name, e.g. "B-Nutri". */
	@Indexed(unique = true)
	private String name;

	/** Optional human-friendly slug / code. */
	private String code;

	/** Base URL of the store backend, e.g. https://api.bnutritionmart.in */
	private String apiBaseUrl;

	/** Public storefront URL (informational). */
	private String websiteUrl;

	/**
	 * Shared secret the store backend sends in X-Store-Api-Key when calling /api/store-api/*.
	 * Lets us tell stores apart and authorize their subscription calls.
	 */
	@Indexed(unique = true)
	private String apiKey;

	/** Last liveness result. */
	@Builder.Default
	private StoreStatus status = StoreStatus.UNKNOWN;

	/** When the last successful/attempted health check ran. */
	private Instant lastPingAt;

	/** Round-trip time of the last successful health check, in ms (null if last check failed). */
	private Long lastPingLatencyMs;

	/** Whether this store is allowed to operate at all (super-admin can disable). */
	@Builder.Default
	private boolean enabled = true;

	private String contactEmail;
	private String contactPhone;
	private String notes;

	/** Complimentary maintenance ends at this instant (store admin sees coverage until then). Super-admin can change. */
	private Instant maintenanceFreeUntil;

	/**
	 * Which subscription services this store offers, by SubscriptionProduct code
	 * (e.g. {@code PAYMENT}, {@code MAINTENANCE}). Controls what the store's app sees
	 * and what cards the super-admin StoreDetail shows. Defaults to both for backward
	 * compatibility with stores registered before this field existed.
	 */
	@Builder.Default
	private List<String> enabledServices = List.of("PAYMENT", "MAINTENANCE");

	// ----- Connector handshake state -----
	/** Whether the last handshake with this store's connector succeeded. */
	@Builder.Default
	private boolean connected = false;
	/** Plugins the store reported as installed (e.g. ["RCA"]). Drives the UI services list. */
	private List<String> installedPlugins;
	/** Connector version reported by the store. */
	private String connectorVersion;
	private Instant lastHandshakeAt;

	private Instant createdAt;
	private Instant updatedAt;
}
