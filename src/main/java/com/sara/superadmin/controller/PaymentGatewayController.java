package com.sara.superadmin.controller;

import com.sara.superadmin.dto.PaymentGatewayConfigDto;
import com.sara.superadmin.service.PaymentGatewayConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Super-admin's own payment-gateway credentials (Razorpay + Cashfree) to collect subscription payments. */
@RestController
@RequestMapping("/api/super/payment-gateway")
public class PaymentGatewayController {

	private final PaymentGatewayConfigService configService;

	public PaymentGatewayController(PaymentGatewayConfigService configService) {
		this.configService = configService;
	}

	@GetMapping
	public PaymentGatewayConfigDto get() {
		return configService.getConfigMasked();
	}

	@PutMapping
	public PaymentGatewayConfigDto update(@Valid @RequestBody PaymentGatewayConfigDto dto) {
		return configService.updateConfig(dto);
	}
}
