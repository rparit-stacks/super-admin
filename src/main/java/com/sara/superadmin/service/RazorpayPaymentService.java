package com.sara.superadmin.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.sara.superadmin.dto.PaymentGatewayConfigDto;
import com.sara.superadmin.model.PaymentGatewayConfig;
import com.sara.superadmin.repository.PaymentGatewayConfigRepository;
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
import java.time.Instant;
import java.util.Map;

/**
 * Uses the super-admin's OWN Razorpay credentials to create and verify
 * subscription payment orders. Singleton config in Mongo.
 */
@Service
public class RazorpayPaymentService {

	private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentService.class);

	private final PaymentGatewayConfigRepository configRepository;

	public RazorpayPaymentService(PaymentGatewayConfigRepository configRepository) {
		this.configRepository = configRepository;
	}

	private PaymentGatewayConfig requireEnabledConfig() {
		PaymentGatewayConfig cfg = configRepository.findFirstByOrderByIdAsc()
				.orElseThrow(() -> ApiException.server("Razorpay is not configured in super-admin"));
		if (!cfg.isEnabled() || cfg.getRazorpayKeyId() == null || cfg.getRazorpayKeyId().isBlank()
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

	// ---------------- Config management (super-admin) ----------------

	public PaymentGatewayConfigDto getConfigMasked() {
		PaymentGatewayConfig cfg = configRepository.findFirstByOrderByIdAsc()
				.orElse(PaymentGatewayConfig.builder().enabled(false).build());
		String maskedSecret = (cfg.getRazorpayKeySecret() != null && !cfg.getRazorpayKeySecret().isBlank())
				? PaymentGatewayConfigDto.SECRET_MASK : "";
		return new PaymentGatewayConfigDto(
				cfg.getRazorpayKeyId() == null ? "" : cfg.getRazorpayKeyId(),
				maskedSecret,
				cfg.isEnabled());
	}

	public PaymentGatewayConfigDto updateConfig(PaymentGatewayConfigDto dto) {
		PaymentGatewayConfig cfg = configRepository.findFirstByOrderByIdAsc()
				.orElseGet(() -> PaymentGatewayConfig.builder().build());

		cfg.setRazorpayKeyId(dto.razorpayKeyId());
		// Empty or masked secret means "keep existing".
		if (dto.razorpayKeySecret() != null
				&& !dto.razorpayKeySecret().isBlank()
				&& !PaymentGatewayConfigDto.SECRET_MASK.equals(dto.razorpayKeySecret())) {
			cfg.setRazorpayKeySecret(dto.razorpayKeySecret());
		}
		cfg.setEnabled(dto.enabled());
		cfg.setUpdatedAt(Instant.now());
		configRepository.save(cfg);
		return getConfigMasked();
	}
}
