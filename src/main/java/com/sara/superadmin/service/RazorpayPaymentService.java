package com.sara.superadmin.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.sara.superadmin.model.PaymentGatewayConfig;
import com.sara.superadmin.web.ApiException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Uses the super-admin's OWN Razorpay credentials to create and verify
 * subscription payment orders. Reads the singleton config via
 * {@link PaymentGatewayConfigService}; the config screen is managed there.
 */
@Service
public class RazorpayPaymentService {

	private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentService.class);

	private final PaymentGatewayConfigService configService;

	public RazorpayPaymentService(PaymentGatewayConfigService configService) {
		this.configService = configService;
	}

	public boolean isEnabled() {
		PaymentGatewayConfig cfg = configService.getOrEmpty();
		return cfg.isRazorpayEnabled()
				&& cfg.getRazorpayKeyId() != null && !cfg.getRazorpayKeyId().isBlank()
				&& cfg.getRazorpayKeySecret() != null && !cfg.getRazorpayKeySecret().isBlank();
	}

	private PaymentGatewayConfig requireEnabledConfig() {
		PaymentGatewayConfig cfg = configService.getOrEmpty();
		if (!cfg.isRazorpayEnabled() || cfg.getRazorpayKeyId() == null || cfg.getRazorpayKeyId().isBlank()
				|| cfg.getRazorpayKeySecret() == null || cfg.getRazorpayKeySecret().isBlank()) {
			throw ApiException.server("Razorpay is not enabled/configured in super-admin");
		}
		return cfg;
	}

	public String getPublicKeyId() {
		return requireEnabledConfig().getRazorpayKeyId();
	}

	/**
	 * Create a Razorpay order for the given INR amount.
	 * @return the Razorpay order id.
	 */
	public String createOrder(BigDecimal amount, String currency, String receipt, Map<String, String> notes) {
		PaymentGatewayConfig cfg = requireEnabledConfig();
		try {
			RazorpayClient client = new RazorpayClient(cfg.getRazorpayKeyId(), cfg.getRazorpayKeySecret());

			// Razorpay expects the smallest currency unit (paise for INR).
			long minor = amount.multiply(BigDecimal.valueOf(100))
					.setScale(0, RoundingMode.HALF_UP).longValueExact();

			JSONObject orderRequest = new JSONObject();
			orderRequest.put("amount", minor);
			orderRequest.put("currency", currency);
			orderRequest.put("receipt", receipt);
			orderRequest.put("payment_capture", true);
			if (notes != null && !notes.isEmpty()) {
				orderRequest.put("notes", new JSONObject(notes));
			}

			Order order = client.orders.create(orderRequest);
			return order.get("id");
		} catch (Exception e) {
			log.error("Failed to create Razorpay order", e);
			throw ApiException.server("Failed to create payment order: " + e.getMessage());
		}
	}

	/**
	 * Verify the Razorpay checkout signature for an order.
	 * Razorpay signs "{order_id}|{payment_id}" with HMAC-SHA256 using the key secret.
	 */
	public boolean verifySignature(String orderId, String paymentId, String signature) {
		PaymentGatewayConfig cfg = requireEnabledConfig();
		if (orderId == null || paymentId == null || signature == null) {
			return false;
		}
		try {
			String payload = orderId + "|" + paymentId;
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(
					cfg.getRazorpayKeySecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				hex.append(Character.forDigit((b >> 4) & 0xF, 16));
				hex.append(Character.forDigit(b & 0xF, 16));
			}
			return hex.toString().equals(signature);
		} catch (Exception e) {
			log.warn("Razorpay signature verification error: {}", e.getMessage());
			return false;
		}
	}
}
