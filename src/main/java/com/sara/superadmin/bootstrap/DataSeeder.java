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

	private String stripTrailingSlash(String url) {
		if (url == null) return null;
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
