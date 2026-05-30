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

	private final SubscriptionRepository repository;
	private final PlanService planService;
	private final RazorpayPaymentService razorpay;
	private final StoreRepository storeRepository;
	private final SubscriptionProductRepository subscriptionProductRepository;

	public SubscriptionService(SubscriptionRepository repository,
							   PlanService planService,
							   RazorpayPaymentService razorpay,
							   StoreRepository storeRepository,
							   SubscriptionProductRepository subscriptionProductRepository) {
		this.repository = repository;
		this.planService = planService;
		this.razorpay = razorpay;
		this.storeRepository = storeRepository;
		this.subscriptionProductRepository = subscriptionProductRepository;
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
				.status(SubscriptionStatus.PENDING)
				.createdAt(now)
				.updatedAt(now)
				.build();
		sub = repository.save(sub);

		BigDecimal price = plan.getPrice() == null ? BigDecimal.ZERO : plan.getPrice();
		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			applyActiveWindow(sub, Instant.now());
			repository.save(sub);
			return new InitiateSubscriptionResponse(
					sub.getId(),
					null,
					null,
					BigDecimal.ZERO,
					plan.getCurrency(),
					gateways.size(),
					true);
		}

		Map<String, String> notes = new HashMap<>();
		notes.put("storeId", storeId);
		notes.put("subscriptionId", sub.getId());
		notes.put("duration", duration.name());
		notes.put("serviceCount", String.valueOf(gateways.size()));

		String receipt = "sub_" + sub.getId();
		String orderId = razorpay.createOrder(plan.getPrice(), plan.getCurrency(), receipt, notes);

		sub.setRazorpayOrderId(orderId);
		sub.setUpdatedAt(Instant.now());
		repository.save(sub);

		return new InitiateSubscriptionResponse(
				sub.getId(),
				orderId,
				razorpay.getPublicKeyId(),
				plan.getPrice(),
				plan.getCurrency(),
				gateways.size(),
				false);
	}

	public InitiateSubscriptionResponse initiateMaintenance(String storeId) {
		if (findActiveMaintenanceSubscription(storeId).isPresent()) {
			throw ApiException.badRequest("Maintenance plan is already active for this store");
		}
		SubscriptionProduct product = subscriptionProductRepository.findByCode("MAINTENANCE")
				.orElseThrow(() -> ApiException.notFound("Maintenance product is not configured"));
		BigDecimal price = product.getMonthlyPrice() != null
				? product.getMonthlyPrice()
				: new BigDecimal("2500");

		Instant now = Instant.now();
		Subscription sub = Subscription.builder()
				.storeId(storeId)
				.duration(PlanDuration.MONTHLY)
				.serviceCount(0)
				.selectedGateways(List.of())
				.amount(price)
				.currency("INR")
				.productLine(PRODUCT_MAINTENANCE)
				.status(SubscriptionStatus.PENDING)
				.createdAt(now)
				.updatedAt(now)
				.build();
		sub = repository.save(sub);

		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			applyActiveWindow(sub, Instant.now());
			repository.save(sub);
			return new InitiateSubscriptionResponse(
					sub.getId(),
					null,
					null,
					BigDecimal.ZERO,
					"INR",
					0,
					true);
		}

		Map<String, String> notes = new HashMap<>();
		notes.put("storeId", storeId);
		notes.put("subscriptionId", sub.getId());
		notes.put("productLine", PRODUCT_MAINTENANCE);

		String receipt = "maint_" + sub.getId();
		String orderId = razorpay.createOrder(price, "INR", receipt, notes);
		sub.setRazorpayOrderId(orderId);
		sub.setUpdatedAt(Instant.now());
		repository.save(sub);

		return new InitiateSubscriptionResponse(
				sub.getId(),
				orderId,
				razorpay.getPublicKeyId(),
				price,
				"INR",
				0,
				false);
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
		if (!Objects.equals(req.razorpayOrderId(), sub.getRazorpayOrderId())) {
			throw ApiException.badRequest("Order id mismatch");
		}
		if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
			return SubscriptionDto.from(sub); // idempotent
		}

		boolean valid = razorpay.verifySignature(
				req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());

		Instant now = Instant.now();
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
