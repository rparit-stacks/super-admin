package com.sara.superadmin.controller;

import com.sara.superadmin.dto.InitiateSubscriptionRequest;
import com.sara.superadmin.dto.InitiateSubscriptionResponse;
import com.sara.superadmin.dto.PlanDto;
import com.sara.superadmin.dto.SubscriptionDto;
import com.sara.superadmin.dto.SubscriptionProductDto;
import com.sara.superadmin.dto.VerifySubscriptionRequest;
import com.sara.superadmin.security.StoreApiKeyFilter;
import com.sara.superadmin.service.PlanService;
import com.sara.superadmin.service.SubscriptionProductService;
import com.sara.superadmin.service.SubscriptionService;
import com.sara.superadmin.web.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoints called BY a store backend, authenticated via X-Store-Api-Key
 * (resolved by StoreApiKeyFilter, which sets the storeId request attribute).
 */
@RestController
@RequestMapping("/api/store-api")
public class StoreApiController {

	private final PlanService planService;
	private final SubscriptionService subscriptionService;
	private final SubscriptionProductService subscriptionProductService;

	public StoreApiController(PlanService planService,
							  SubscriptionService subscriptionService,
							  SubscriptionProductService subscriptionProductService) {
		this.planService = planService;
		this.subscriptionService = subscriptionService;
		this.subscriptionProductService = subscriptionProductService;
	}

	private String storeId(HttpServletRequest request) {
		Object id = request.getAttribute(StoreApiKeyFilter.STORE_ID_ATTR);
		if (id == null) {
			throw ApiException.unauthorized("Missing or invalid store API key");
		}
		return id.toString();
	}

	/** Pricing matrix the store frontend renders on its Plans page. */
	@GetMapping("/plans")
	public List<PlanDto> plans() {
		return planService.listPlans();
	}

	/** Subscription types the store admin may offer (picker cards); titles and flows from DB. */
	@GetMapping("/subscription-products")
	public List<SubscriptionProductDto> subscriptionProducts() {
		return subscriptionProductService.listVisibleForStore();
	}

	/** Start a subscription purchase; returns Razorpay order details for checkout. */
	@PostMapping("/subscriptions/initiate")
	public InitiateSubscriptionResponse initiate(HttpServletRequest request,
												 @Valid @RequestBody InitiateSubscriptionRequest body) {
		return subscriptionService.initiate(storeId(request), body);
	}

	/** Confirm payment; on success the subscription becomes ACTIVE. */
	@PostMapping("/subscriptions/verify")
	public SubscriptionDto verify(HttpServletRequest request,
								  @Valid @RequestBody VerifySubscriptionRequest body) {
		return subscriptionService.verify(storeId(request), body);
	}

	/**
	 * The store polls this to learn which gateways are unlocked. Returns
	 * {active: false} when there is no live subscription (COD-only).
	 */
	@GetMapping("/subscriptions/active")
	public Map<String, Object> active(HttpServletRequest request) {
		String storeId = storeId(request);
		return subscriptionService.findActiveSubscription(storeId)
				.map(s -> Map.<String, Object>of(
						"active", true,
						"subscription", SubscriptionDto.from(s)))
				.orElse(Map.of("active", false));
	}

	/** Recent subscription rows for the store admin (includes ended / failed). */
	@GetMapping("/subscriptions/history")
	public List<SubscriptionDto> subscriptionHistory(HttpServletRequest request) {
		return subscriptionService.listForStore(storeId(request)).stream().limit(40).toList();
	}
}
