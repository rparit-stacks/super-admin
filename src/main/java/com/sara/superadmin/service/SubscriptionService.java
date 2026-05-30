package com.sara.superadmin.service;

import com.sara.superadmin.dto.InitiateSubscriptionRequest;
import com.sara.superadmin.dto.InitiateSubscriptionResponse;
import com.sara.superadmin.dto.SubscriptionDto;
import com.sara.superadmin.dto.VerifySubscriptionRequest;
import com.sara.superadmin.model.PaymentGateway;
import com.sara.superadmin.model.Plan;
import com.sara.superadmin.model.PlanDuration;
import com.sara.superadmin.model.Subscription;
import com.sara.superadmin.model.SubscriptionStatus;
import com.sara.superadmin.repository.SubscriptionRepository;
import com.sara.superadmin.web.ApiException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SubscriptionService {

	/** Minimum number of gateways a store must select. */
	public static final int MIN_SERVICES = 2;
	public static final int MAX_SERVICES = 4;

	private final SubscriptionRepository repository;
	private final PlanService planService;
	private final RazorpayPaymentService razorpay;

	public SubscriptionService(SubscriptionRepository repository,
							   PlanService planService,
							   RazorpayPaymentService razorpay) {
		this.repository = repository;
		this.planService = planService;
		this.razorpay = razorpay;
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

	/** The store's current ACTIVE, non-expired subscription, if any. */
	public Optional<Subscription> findActiveSubscription(String storeId) {
		Instant now = Instant.now();
		return repository.findByStoreIdAndStatus(storeId, SubscriptionStatus.ACTIVE).stream()
				.filter(s -> s.getEndDate() == null || s.getEndDate().isAfter(now))
				.max((a, b) -> {
					Instant ea = a.getEndDate(), eb = b.getEndDate();
					if (ea == null) return 1;   // lifetime ranks highest
					if (eb == null) return -1;
					return ea.compareTo(eb);
				});
	}

	// ---------------- Purchase flow ----------------

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
				.status(SubscriptionStatus.PENDING)
				.createdAt(now)
				.updatedAt(now)
				.build();
		sub = repository.save(sub);

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
				gateways.size());
	}

	public SubscriptionDto verify(String storeId, VerifySubscriptionRequest req) {
		Subscription sub = repository.findById(req.subscriptionId())
				.orElseThrow(() -> ApiException.notFound("Subscription not found"));

		if (!sub.getStoreId().equals(storeId)) {
			throw ApiException.unauthorized("Subscription does not belong to this store");
		}
		if (!req.razorpayOrderId().equals(sub.getRazorpayOrderId())) {
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
		sub.setStatus(SubscriptionStatus.ACTIVE);
		sub.setStartDate(now);
		sub.setEndDate(sub.getDuration().isLifetime()
				? null
				: now.plus(sub.getDuration().getMonths() * 30L, ChronoUnit.DAYS));
		sub.setUpdatedAt(now);
		repository.save(sub);

		return SubscriptionDto.from(sub);
	}
}
