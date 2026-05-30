package com.sara.superadmin.controller;

import com.sara.superadmin.dto.SubscriptionDto;
import com.sara.superadmin.service.SubscriptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Super-admin view of all subscriptions, optionally filtered by store. */
@RestController
@RequestMapping("/api/super/subscriptions")
public class SubscriptionAdminController {

	private final SubscriptionService subscriptionService;

	public SubscriptionAdminController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	@GetMapping
	public List<SubscriptionDto> list(@RequestParam(required = false) String storeId) {
		if (storeId != null && !storeId.isBlank()) {
			return subscriptionService.listForStore(storeId);
		}
		return subscriptionService.listAll();
	}
}
