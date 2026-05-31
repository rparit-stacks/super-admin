package com.sara.superadmin.service;

import com.sara.superadmin.dto.InitiateSubscriptionRequest;
import com.sara.superadmin.dto.InitiateSubscriptionResponse;
import com.sara.superadmin.dto.SubscriptionDto;
import com.sara.superadmin.dto.VerifySubscriptionRequest;
import com.sara.superadmin.model.PaymentGateway;
import com.sara.superadmin.model.Plan;
import com.sara.superadmin.model.PlanDuration;
import com.sara.superadmin.model.Store;
import com.sara.superadmin.model.Subscription;
import com.sara.superadmin.model.SubscriptionProduct;
import com.sara.superadmin.model.SubscriptionStatus;
import com.sara.superadmin.repository.StoreRepository;
import com.sara.superadmin.repository.SubscriptionProductRepository;
import com.sara.superadmin.repository.SubscriptionRepository;
import com.sara.superadmin.web.ApiException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class SubscriptionService {

	/** Minimum number of gateways a store must select. */
	public static final int MIN_SERVICES = 2;
	public static final int MAX_SERVICES = 4;

	public static final String PRODUCT_PAYMENT = "PAYMENT";
	public static final String PRODUCT_MAINTENANCE = "MAINTENANCE";

	public static final String PROVIDER_RAZORPAY = "RAZORPAY";
	public static final String PROVIDER_CASHFREE = "CASHFREE";

	private final SubscriptionRepository repository;
	private final PlanService planService;
	private final RazorpayPaymentService razorpay;
	private final CashfreePaymentService cashfree;
	private final StoreRepository storeRepository;
	private final SubscriptionProductRepository subscriptionProductRepository;

	public SubscriptionService(SubscriptionRepository repository,
							   PlanService planService,
							   RazorpayPaymentService razorpay,
							   CashfreePaymentService cashfree,
							   StoreRepository storeRepository,
							   SubscriptionProductRepository subscriptionProductRepository) {
		this.repository = repository;
		this.planService = planService;
		this.razorpay = razorpay;
		this.cashfree = cashfree;
		this.storeRepository = storeRepository;
		this.subscriptionProductRepository = subscriptionProductRepository;
	}

	/** Resolve and validate the requested billing provider (defaults to Razorpay). */
	private String resolveEnabledProvider(String requested) {
		String provider = (requested != null && PROVIDER_CASHFREE.equalsIgnoreCase(requested))
				? PROVIDER_CASHFREE : PROVIDER_RAZORPAY;
		if (PROVIDER_CASHFREE.equals(provider)) {
			if (!cashfree.isEnabled()) {
				throw ApiException.badRequest("Cashfree is not available right now");
			}
		} else if (!razorpay.isEnabled()) {
			throw ApiException.badRequest("Razorpay is not available right now");
		}
		return provider;
	}

	// ---------------- Queries ----------------

	public List<SubscriptionDto> listForStore(String storeId) {
		return repository.findByStoreIdOrderByCreatedAtDesc(storeId).stream()
				.map(SubscriptionDto::from)
				.toList();
	}

	public List<SubscriptionDto> listAll() {
		return repository.findAll().stream()
				.sorted((a, b) -> {
					Instant ca = a.getCreatedAt(), cb = b.getCreatedAt();
					if (ca == null) return 1;
					if (cb == null) return -1;
					return cb.compareTo(ca);
				})
				.map(SubscriptionDto::from)
				.toList();
	}

	private static boolean isPaymentLine(Subscription s) {
		String pl = s.getProductLine();
		return pl == null || PRODUCT_PAYMENT.equalsIgnoreCase(pl);
	}

	private static boolean isMaintenanceLine(Subscription s) {
		return PRODUCT_MAINTENANCE.equalsIgnoreCase(s.getProductLine());
	}

	/** Active payment-gateway subscription (excludes maintenance rows). */
	public Optional<Subscription> findActiveSubscription(String storeId) {
		Instant now = Instant.now();
		return repository.findByStoreIdAndStatus(storeId, SubscriptionStatus.ACTIVE).stream()
				.filter(SubscriptionService::isPaymentLine)
				.filter(s -> s.getEndDate() == null || s.getEndDate().isAfter(now))
				.max((a, b) -> {
					Instant ea = a.getEndDate(), eb = b.getEndDate();
					if (ea == null) return 1;
					if (eb == null) return -1;
					return ea.compareTo(eb);
				});
	}

	public Optional<Subscription> findActiveMaintenanceSubscription(String storeId) {
		Instant now = Instant.now();
		return repository.findByStoreIdAndStatus(storeId, SubscriptionStatus.ACTIVE).stream()
				.filter(SubscriptionService::isMaintenanceLine)
				.filter(s -> s.getEndDate() == null || s.getEndDate().isAfter(now))
				.findFirst();
	}

	// ---------------- Maintenance status (store API) ----------------

	public Map<String, Object> getMaintenanceStatus(String storeId) {
		Store store = storeRepository.findById(storeId)
				.orElseThrow(() -> ApiException.notFound("Store not found"));
		Instant freeUntil = store.getMaintenanceFreeUntil();
		Instant now = Instant.now();
		boolean withinComplimentary = freeUntil != null && now.isBefore(freeUntil);

		SubscriptionProduct product = subscriptionProductRepository.findByCode("MAINTENANCE").orElse(null);
		BigDecimal monthly = product != null && product.getMonthlyPrice() != null
				? product.getMonthlyPrice()
				: new BigDecimal("2500");
		String title = product != null && product.getTitle() != null
				? product.getTitle()
				: "Website maintenance";

		Optional<Subscription> paid = findActiveMaintenanceSubscription(storeId);
		boolean paidActive = paid.isPresent();
		boolean coverage = withinComplimentary || paidActive;

		Map<String, Object> m = new HashMap<>();
		m.put("maintenanceFreeUntil", freeUntil);
		m.put("withinComplimentaryPeriod", withinComplimentary);
		m.put("monthlyPrice", monthly);
		m.put("currency", "INR");
		m.put("productTitle", title);
		m.put("paidMaintenanceActive", paidActive);
		m.put("coverageActive", coverage);
		paid.ifPresent(s -> {
			m.put("paidUntil", s.getEndDate());
			m.put("paidAmount", s.getAmount());
			m.put("paidSubscriptionId", s.getId());
		});
		return m;
	}

	// ---------------- Purchase flow (payment gateways) ----------------

	public InitiateSubscriptionResponse initiate(String storeId, InitiateSubscriptionRequest req) {
		List<PaymentGateway> gateways = req.selectedGateways() == null ? List.of()
				: req.selectedGateways().stream().distinct().toList();

		if (gateways.size() < MIN_SERVICES) {
			throw ApiException.badRequest("Select at least " + MIN_SERVICES + " payment gateways");
		}
		if (gateways.size() > MAX_SERVICES) {
			throw ApiException.badRequest("Cannot select more than " + MAX_SERVICES + " gateways");
		}

		String provider = resolveEnabledProvider(req.paymentProvider());

		PlanDuration duration = req.duration();
		Plan plan = planService.resolve(duration, gateways.size());

		Instant now = Instant.now();
		Subscription sub = Subscription.builder()
				.storeId(storeId)
				.duration(duration)
				.serviceCount(gateways.size())
				.selectedGateways(gateways)
				.amount(plan.getPrice())
				.currency(plan.getCurrency())
				.productLine(PRODUCT_PAYMENT)
				.paymentProvider(provider)
				.status(SubscriptionStatus.PENDING)
				.createdAt(now)
				.updatedAt(now)
				.build();
		sub = repository.save(sub);

		BigDecimal price = plan.getPrice() == null ? BigDecimal.ZERO : plan.getPrice();
		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			applyActiveWindow(sub, Instant.now());
			repository.save(sub);
			return InitiateSubscriptionResponse.free(sub.getId(), plan.getCurrency(), gateways.size());
		}

		Map<String, String> notes = new HashMap<>();
		notes.put("storeId", storeId);
		notes.put("subscriptionId", sub.getId());
		notes.put("duration", duration.name());
		notes.put("serviceCount", String.valueOf(gateways.size()));

		return createOrderResponse(sub, provider, price, plan.getCurrency(), "sub_" + sub.getId(),
				notes, gateways.size());
	}

	public InitiateSubscriptionResponse initiateMaintenance(String storeId, String requestedProvider) {
		if (findActiveMaintenanceSubscription(storeId).isPresent()) {
			throw ApiException.badRequest("Maintenance plan is already active for this store");
		}
		SubscriptionProduct product = subscriptionProductRepository.findByCode("MAINTENANCE")
				.orElseThrow(() -> ApiException.notFound("Maintenance product is not configured"));
		BigDecimal price = product.getMonthlyPrice() != null
				? product.getMonthlyPrice()
				: new BigDecimal("2500");

		String provider = resolveEnabledProvider(requestedProvider);

		Instant now = Instant.now();
		Subscription sub = Subscription.builder()
				.storeId(storeId)
				.duration(PlanDuration.MONTHLY)
				.serviceCount(0)
				.selectedGateways(List.of())
				.amount(price)
				.currency("INR")
				.productLine(PRODUCT_MAINTENANCE)
				.paymentProvider(provider)
				.status(SubscriptionStatus.PENDING)
				.createdAt(now)
				.updatedAt(now)
				.build();
		sub = repository.save(sub);

		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			applyActiveWindow(sub, Instant.now());
			repository.save(sub);
			return InitiateSubscriptionResponse.free(sub.getId(), "INR", 0);
		}

		Map<String, String> notes = new HashMap<>();
		notes.put("storeId", storeId);
		notes.put("subscriptionId", sub.getId());
		notes.put("productLine", PRODUCT_MAINTENANCE);

		return createOrderResponse(sub, provider, price, "INR", "maint_" + sub.getId(), notes, 0);
	}

	/** Create the provider order, persist its linkage on the subscription, and build the response. */
	private InitiateSubscriptionResponse createOrderResponse(Subscription sub, String provider, BigDecimal price,
															 String currency, String receipt,
															 Map<String, String> notes, int serviceCount) {
		if (PROVIDER_CASHFREE.equals(provider)) {
			CashfreePaymentService.CashfreeOrder order = cashfree.createOrder(price, currency, receipt, notes);
			sub.setCashfreeOrderId(order.orderId());
			sub.setCashfreePaymentSessionId(order.paymentSessionId());
			sub.setUpdatedAt(Instant.now());
			repository.save(sub);
			return InitiateSubscriptionResponse.cashfree(sub.getId(), order.orderId(), order.paymentSessionId(),
					cashfree.getEnv(), price, currency, serviceCount);
		}

		String orderId = razorpay.createOrder(price, currency, receipt, notes);
		sub.setRazorpayOrderId(orderId);
		sub.setUpdatedAt(Instant.now());
		repository.save(sub);
		return InitiateSubscriptionResponse.razorpay(sub.getId(), orderId, razorpay.getPublicKeyId(),
				price, currency, serviceCount);
	}

	/** Sets ACTIVE status and subscription window from {@code now} (used after pay or for free plans). */
	private void applyActiveWindow(Subscription sub, Instant now) {
		sub.setStatus(SubscriptionStatus.ACTIVE);
		sub.setStartDate(now);
		if (sub.getDuration() == PlanDuration.LIFETIME) {
			sub.setEndDate(null);
		} else if (sub.getDuration() == PlanDuration.MONTHLY) {
			sub.setEndDate(now.atZone(ZoneOffset.UTC).plusMonths(1).toInstant());
		} else {
			sub.setEndDate(now.plus(sub.getDuration().getMonths() * 30L, ChronoUnit.DAYS));
		}
		sub.setUpdatedAt(now);
	}

	public SubscriptionDto verify(String storeId, VerifySubscriptionRequest req) {
		Subscription sub = repository.findById(req.subscriptionId())
				.orElseThrow(() -> ApiException.notFound("Subscription not found"));

		if (!sub.getStoreId().equals(storeId)) {
			throw ApiException.unauthorized("Subscription does not belong to this store");
		}
		if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
			return SubscriptionDto.from(sub); // idempotent
		}

		String provider = sub.getPaymentProvider() == null ? PROVIDER_RAZORPAY : sub.getPaymentProvider();
		Instant now = Instant.now();

		if (PROVIDER_CASHFREE.equals(provider)) {
			if (!Objects.equals(req.cashfreeOrderId(), sub.getCashfreeOrderId())) {
				throw ApiException.badRequest("Order id mismatch");
			}
			boolean paid = cashfree.verifyPayment(sub.getCashfreeOrderId());
			if (!paid) {
				sub.setStatus(SubscriptionStatus.FAILED);
				sub.setUpdatedAt(now);
				repository.save(sub);
				throw ApiException.badRequest("Cashfree payment not confirmed");
			}
			applyActiveWindow(sub, now);
			repository.save(sub);
			return SubscriptionDto.from(sub);
		}

		// Razorpay
		if (!Objects.equals(req.razorpayOrderId(), sub.getRazorpayOrderId())) {
			throw ApiException.badRequest("Order id mismatch");
		}
		boolean valid = razorpay.verifySignature(
				req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
		if (!valid) {
			sub.setStatus(SubscriptionStatus.FAILED);
			sub.setUpdatedAt(now);
			repository.save(sub);
			throw ApiException.badRequest("Payment signature verification failed");
		}

		sub.setRazorpayPaymentId(req.razorpayPaymentId());
		sub.setRazorpaySignature(req.razorpaySignature());
		applyActiveWindow(sub, now);
		repository.save(sub);

		return SubscriptionDto.from(sub);
	}
}
