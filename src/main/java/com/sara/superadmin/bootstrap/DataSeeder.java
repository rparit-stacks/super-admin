package com.sara.superadmin.bootstrap;

import com.sara.superadmin.model.Plan;
import com.sara.superadmin.model.PlanDuration;
import com.sara.superadmin.model.Store;
import com.sara.superadmin.model.StoreStatus;
import com.sara.superadmin.model.SuperAdmin;
import com.sara.superadmin.model.SubscriptionProduct;
import com.sara.superadmin.repository.PlanRepository;
import com.sara.superadmin.repository.StoreRepository;
import com.sara.superadmin.repository.SubscriptionProductRepository;
import com.sara.superadmin.repository.SuperAdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Seeds initial data on first boot (idempotent):
 *   - the 9-row pricing matrix (default prices)
 *   - the default super-admin account
 *   - the B-Nutri store
 * Existing data is never overwritten.
 */
@Component
public class DataSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

	private final PlanRepository planRepository;
	private final SubscriptionProductRepository subscriptionProductRepository;
	private final SuperAdminRepository superAdminRepository;
	private final StoreRepository storeRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${super-admin.seed.username}")
	private String seedUsername;
	@Value("${super-admin.seed.password}")
	private String seedPassword;
	@Value("${super-admin.seed.store.name}")
	private String seedStoreName;
	@Value("${super-admin.seed.store.api-base-url}")
	private String seedStoreApiBaseUrl;
	@Value("${super-admin.store-api-key}")
	private String globalStoreApiKey;

	@Value("${super-admin.seed.store2.name:}")
	private String seedStore2Name;
	@Value("${super-admin.seed.store2.code:}")
	private String seedStore2Code;
	@Value("${super-admin.seed.store2.api-base-url:}")
	private String seedStore2ApiBaseUrl;
	@Value("${super-admin.seed.store2.website-url:}")
	private String seedStore2WebsiteUrl;
	@Value("${super-admin.seed.store2.api-key:}")
	private String seedStore2ApiKey;

	public DataSeeder(PlanRepository planRepository,
					  SubscriptionProductRepository subscriptionProductRepository,
					  SuperAdminRepository superAdminRepository,
					  StoreRepository storeRepository,
					  PasswordEncoder passwordEncoder) {
		this.planRepository = planRepository;
		this.subscriptionProductRepository = subscriptionProductRepository;
		this.superAdminRepository = superAdminRepository;
		this.storeRepository = storeRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		seedPlans();
		seedSubscriptionProducts();
		seedSuperAdmin();
		seedStore();
		seedStore2();
		ensureMaintenanceProduct();
		ensureBnuriComplimentaryMaintenance();
		ensureStore2ComplimentaryMaintenance();
		ensureStore2Services();
	}

	private void seedPlans() {
		if (planRepository.count() > 0) {
			return;
		}
		// duration, serviceCount -> price (INR), per the agreed pricing matrix.
		seedPlan(PlanDuration.SIX_MONTH, 2, 3299);
		seedPlan(PlanDuration.SIX_MONTH, 3, 3699);
		seedPlan(PlanDuration.SIX_MONTH, 4, 3999);

		seedPlan(PlanDuration.ONE_YEAR, 2, 4299);
		seedPlan(PlanDuration.ONE_YEAR, 3, 4699);
		seedPlan(PlanDuration.ONE_YEAR, 4, 4999);

		seedPlan(PlanDuration.LIFETIME, 2, 5299);
		seedPlan(PlanDuration.LIFETIME, 3, 5699);
		seedPlan(PlanDuration.LIFETIME, 4, 5999);

		log.info("Seeded {} default plans", planRepository.count());
	}

	private void seedSubscriptionProducts() {
		if (subscriptionProductRepository.count() > 0) {
			return;
		}
		subscriptionProductRepository.save(SubscriptionProduct.builder()
				.code("PAYMENT")
				.title("Payment subscription")
				.description(
						"Unlock online payment gateways for your store (Razorpay, Stripe, PayU, Partial COD). "
								+ "Gateway keys and toggles stay under Payment Settings.")
				.visibleToStores(true)
				.sortOrder(0)
				.checkoutFlow("PAYMENT_GATEWAYS")
				.build());
		subscriptionProductRepository.save(SubscriptionProduct.builder()
				.code("CHAT_AI")
				.title("Chat & AI subscription")
				.description("AI assistant and customer chat for your storefront. Checkout will be enabled when this product launches.")
				.visibleToStores(true)
				.sortOrder(1)
				.checkoutFlow(null)
				.build());
		subscriptionProductRepository.save(SubscriptionProduct.builder()
				.code("MAINTENANCE")
				.title("Website maintenance")
				.description(
						"Server, hosting, monitoring, backups, security updates, DB care, deployments, performance, "
								+ "reporting, and priority technical support. One plan: ₹2,500/month after any "
								+ "complimentary period configured by support.")
				.visibleToStores(true)
				.sortOrder(2)
				.monthlyPrice(BigDecimal.valueOf(2500))
				.checkoutFlow("MAINTENANCE_MONTHLY")
				.build());
		log.info("Seeded {} subscription product rows", subscriptionProductRepository.count());
	}

	private void seedPlan(PlanDuration duration, int serviceCount, long price) {
		planRepository.save(Plan.builder()
				.duration(duration)
				.serviceCount(serviceCount)
				.price(BigDecimal.valueOf(price))
				.currency("INR")
				.active(true)
				.build());
	}

	private void seedSuperAdmin() {
		if (superAdminRepository.count() > 0) {
			return;
		}
		superAdminRepository.save(SuperAdmin.builder()
				.username(seedUsername)
				.passwordHash(passwordEncoder.encode(seedPassword))
				.name("Super Admin")
				.enabled(true)
				.createdAt(Instant.now())
				.build());
		log.info("Seeded default super-admin '{}'", seedUsername);
	}

	private void seedStore() {
		if (storeRepository.existsByName(seedStoreName)) {
			return;
		}
		// Use the configured global store key so the store backend can authenticate
		// immediately; super-admin can rotate it later from the UI.
		String apiKey = (globalStoreApiKey != null && !globalStoreApiKey.isBlank())
				? globalStoreApiKey
				: "sk_store_" + UUID.randomUUID().toString().replace("-", "");

		Instant now = Instant.now();
		storeRepository.save(Store.builder()
				.name(seedStoreName)
				.code("b-nutri")
				.apiBaseUrl(stripTrailingSlash(seedStoreApiBaseUrl))
				.apiKey(apiKey)
				.status(StoreStatus.UNKNOWN)
				.enabled(true)
				.createdAt(now)
				.updatedAt(now)
				.build());
		log.info("Seeded store '{}' ({})", seedStoreName, seedStoreApiBaseUrl);
	}

	/**
	 * Register the second store (Studio Sara) on first boot with its OWN per-store API key
	 * and base URL. Idempotent: skipped if a store with this name/code already exists, or if
	 * store2 config is not provided.
	 */
	private void seedStore2() {
		if (seedStore2Name == null || seedStore2Name.isBlank()
				|| seedStore2ApiBaseUrl == null || seedStore2ApiBaseUrl.isBlank()) {
			return;
		}
		boolean exists = storeRepository.existsByName(seedStore2Name)
				|| (seedStore2Code != null && !seedStore2Code.isBlank()
						&& storeRepository.findByCode(seedStore2Code).isPresent());
		if (exists) {
			return;
		}
		String apiKey = (seedStore2ApiKey != null && !seedStore2ApiKey.isBlank())
				? seedStore2ApiKey
				: "sk_store_" + UUID.randomUUID().toString().replace("-", "");

		Instant now = Instant.now();
		storeRepository.save(Store.builder()
				.name(seedStore2Name)
				.code(seedStore2Code != null && !seedStore2Code.isBlank() ? seedStore2Code : "studio-sara")
				.apiBaseUrl(stripTrailingSlash(seedStore2ApiBaseUrl))
				.websiteUrl(seedStore2WebsiteUrl != null && !seedStore2WebsiteUrl.isBlank() ? seedStore2WebsiteUrl : null)
				.apiKey(apiKey)
				.status(StoreStatus.UNKNOWN)
				.enabled(true)
				// Studio Sara offers maintenance + Root Cause Analyzer.
				.enabledServices(java.util.List.of("MAINTENANCE", "RCA"))
				.createdAt(now)
				.updatedAt(now)
				.build());
		log.info("Seeded store '{}' ({})", seedStore2Name, seedStore2ApiBaseUrl);
	}

	/**
	 * Studio Sara offers only MAINTENANCE. Set this on the existing row if it was seeded
	 * before {@code enabledServices} existed (or still carries the default both-services list).
	 */
	private void ensureStore2Services() {
		String code = (seedStore2Code != null && !seedStore2Code.isBlank()) ? seedStore2Code : "studio-sara";
		storeRepository.findByCode(code).ifPresent(store -> {
			java.util.List<String> svc = store.getEnabledServices();
			boolean isDefaultBoth = svc != null && svc.size() == 2
					&& svc.contains("PAYMENT") && svc.contains("MAINTENANCE");
			boolean missingRca = svc == null || !svc.contains("RCA");
			if (svc == null || svc.isEmpty() || isDefaultBoth) {
				store.setEnabledServices(java.util.List.of("MAINTENANCE", "RCA"));
				store.setUpdatedAt(Instant.now());
				storeRepository.save(store);
				log.info("Set enabledServices=[MAINTENANCE, RCA] for store code {}", code);
			} else if (missingRca) {
				// Sara already customized, just add RCA.
				java.util.List<String> updated = new java.util.ArrayList<>(svc);
				updated.add("RCA");
				store.setEnabledServices(updated);
				store.setUpdatedAt(Instant.now());
				storeRepository.save(store);
				log.info("Added RCA to enabledServices for store code {}", code);
			}
		});
	}

	/**
	 * Adds the MAINTENANCE product if missing (existing DBs that were seeded before this product existed).
	 */
	private void ensureMaintenanceProduct() {
		if (subscriptionProductRepository.findByCode("MAINTENANCE").isPresent()) {
			return;
		}
		subscriptionProductRepository.save(SubscriptionProduct.builder()
				.code("MAINTENANCE")
				.title("Website maintenance")
				.description(
						"Server, hosting, monitoring, backups, security updates, DB care, deployments, performance, "
								+ "reporting, and priority technical support. One plan: ₹2,500/month after any "
								+ "complimentary period configured by support.")
				.visibleToStores(true)
				.sortOrder(2)
				.monthlyPrice(BigDecimal.valueOf(2500))
				.checkoutFlow("MAINTENANCE_MONTHLY")
				.build());
		log.info("Added MAINTENANCE subscription product");
	}

	/**
	 * B-Nutri: default complimentary maintenance end (11 Apr go-live + 2 months → end of 11 Jun IST) when unset.
	 * Super-admin can change {@code maintenanceFreeUntil} on the store at any time.
	 */
	private void ensureBnuriComplimentaryMaintenance() {
		Instant defaultEnd = ZonedDateTime.of(2026, 6, 11, 23, 59, 59, 0, ZoneId.of("Asia/Kolkata")).toInstant();
		storeRepository.findByCode("b-nutri").ifPresentOrElse(
				store -> {
					if (store.getMaintenanceFreeUntil() == null) {
						store.setMaintenanceFreeUntil(defaultEnd);
						store.setUpdatedAt(Instant.now());
						storeRepository.save(store);
						log.info("Set default complimentary maintenance end for store code b-nutri");
					}
				},
				() -> storeRepository.findByName(seedStoreName).ifPresent(store -> {
					if (store.getMaintenanceFreeUntil() == null) {
						store.setMaintenanceFreeUntil(defaultEnd);
						store.setUpdatedAt(Instant.now());
						storeRepository.save(store);
						log.info("Set default complimentary maintenance end for seeded store name");
					}
				}));
	}

	/**
	 * Studio Sara: default complimentary maintenance window when unset. Super-admin can
	 * change {@code maintenanceFreeUntil} on the store from the dashboard at any time.
	 */
	private void ensureStore2ComplimentaryMaintenance() {
		String code = (seedStore2Code != null && !seedStore2Code.isBlank()) ? seedStore2Code : "studio-sara";
		Instant defaultEnd = ZonedDateTime.of(2026, 8, 31, 23, 59, 59, 0, ZoneId.of("Asia/Kolkata")).toInstant();
		storeRepository.findByCode(code).ifPresentOrElse(
				store -> {
					if (store.getMaintenanceFreeUntil() == null) {
						store.setMaintenanceFreeUntil(defaultEnd);
						store.setUpdatedAt(Instant.now());
						storeRepository.save(store);
						log.info("Set default complimentary maintenance end for store code {}", code);
					}
				},
				() -> {
					if (seedStore2Name != null && !seedStore2Name.isBlank()) {
						storeRepository.findByName(seedStore2Name).ifPresent(store -> {
							if (store.getMaintenanceFreeUntil() == null) {
								store.setMaintenanceFreeUntil(defaultEnd);
								store.setUpdatedAt(Instant.now());
								storeRepository.save(store);
								log.info("Set default complimentary maintenance end for store {}", seedStore2Name);
							}
						});
					}
				});
	}

	private String stripTrailingSlash(String url) {
		if (url == null) return null;
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
