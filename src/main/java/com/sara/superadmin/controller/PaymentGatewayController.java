package com.sara.superadmin.controller;

import com.sara.superadmin.dto.PaymentGatewayConfigDto;
import com.sara.superadmin.service.RazorpayPaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Super-admin's own Razorpay credentials (to collect subscription payments). */
@RestController
@RequestMapping("/api/super/payment-gateway")
public class PaymentGatewayController {

	private final RazorpayPaymentService razorpay;

	public PaymentGatewayController(RazorpayPaymentService razorpay) {
		this.razorpay = razorpay;
	}

	@GetMapping
	public PaymentGatewayConfigDto get() {
		return razorpay.getConfigMasked();
	}

	@PutMapping
	public PaymentGatewayConfigDto update(@Valid @RequestBody PaymentGatewayConfigDto dto) {
		return razorpay.updateConfig(dto);
	}
}
