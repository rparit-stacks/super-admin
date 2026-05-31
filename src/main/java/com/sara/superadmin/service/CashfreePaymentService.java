package com.sara.superadmin.service;

import com.sara.superadmin.model.PaymentGatewayConfig;
import com.sara.superadmin.web.ApiException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Uses the super-admin's OWN Cashfree credentials to create and verify
 * subscription payment orders via the Cashfree PG REST API. Reads the singleton
 * config via {@link PaymentGatewayConfigService}.
 *
 * <p>Unlike Razorpay, Cashfree has no client-side HMAC signature to verify;
 * the authoritative check is a server-side fetch of the order's payments.
 */
@Service
public class CashfreePaymentService {

	private static final Logger log = LoggerFactory.getLogger(CashfreePaymentService.class);

	private static final String API_VERSION = "2023-08-01";
	private static final String PROD_BASE = "https://api.cashfree.com";
	private static final String SANDBOX_BASE = "https://sandbox.cashfree.com";

	private final PaymentGatewayConfigService configService;
	private final RestTemplate restTemplate;

	public CashfreePaymentService(PaymentGatewayConfigService configService,
								  RestTemplate cashfreeRestTemplate) {
		this.configService = configService;
		this.restTemplate = cashfreeRestTemplate;
	}

	public boolean isEnabled() {
		PaymentGatewayConfig cfg = configService.getOrEmpty();
		return cfg.isCashfreeEnabled()
				&& cfg.getCashfreeAppId() != null && !cfg.getCashfreeAppId().isBlank()
				&& cfg.getCashfreeSecretKey() != null && !cfg.getCashfreeSecretKey().isBlank();
	}

	private PaymentGatewayConfig requireEnabledConfig() {
		PaymentGatewayConfig cfg = configService.getOrEmpty();
		if (!cfg.isCashfreeEnabled() || cfg.getCashfreeAppId() == null || cfg.getCashfreeAppId().isBlank()
				|| cfg.getCashfreeSecretKey() == null || cfg.getCashfreeSecretKey().isBlank()) {
			throw ApiException.server("Cashfree is not enabled/configured in super-admin");
		}
		return cfg;
	}

	/** {@code "SANDBOX"} or {@code "PRODUCTION"} — exposed so the store checkout picks the right SDK mode. */
	public String getEnv() {
		String env = configService.getOrEmpty().getCashfreeEnv();
		return "SANDBOX".equalsIgnoreCase(env) ? "SANDBOX" : "PRODUCTION";
	}

	private static String baseUrl(PaymentGatewayConfig cfg) {
		return "SANDBOX".equalsIgnoreCase(cfg.getCashfreeEnv()) ? SANDBOX_BASE : PROD_BASE;
	}

	private HttpHeaders headers(PaymentGatewayConfig cfg) {
		HttpHeaders h = new HttpHeaders();
		h.setContentType(MediaType.APPLICATION_JSON);
		h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
		h.set("x-client-id", cfg.getCashfreeAppId());
		h.set("x-client-secret", cfg.getCashfreeSecretKey());
		h.set("x-api-version", API_VERSION);
		return h;
	}

	/**
	 * Create a Cashfree order for the given amount.
	 *
	 * @param orderId our own subscription-derived id (becomes Cashfree {@code order_id})
	 * @return result carrying the Cashfree order id and the payment_session_id the
	 *         store checkout needs to open the SDK.
	 */
	public CashfreeOrder createOrder(BigDecimal amount, String currency, String orderId, Map<String, String> notes) {
		PaymentGatewayConfig cfg = requireEnabledConfig();
		try {
			BigDecimal orderAmount = amount.setScale(2, RoundingMode.HALF_UP);

			String storeId = notes != null ? notes.getOrDefault("storeId", orderId) : orderId;

			JSONObject customer = new JSONObject();
			// Cashfree requires a customer block; ids are alnum/underscore only.
			customer.put("customer_id", sanitizeId("store_" + storeId));
			customer.put("customer_phone", "9999999999");

			JSONObject body = new JSONObject();
			body.put("order_id", orderId);
			body.put("order_amount", orderAmount.doubleValue());
			body.put("order_currency", currency == null ? "INR" : currency);
			body.put("customer_details", customer);
			if (notes != null && !notes.isEmpty()) {
				body.put("order_tags", new JSONObject(notes));
			}

			ResponseEntity<String> resp = restTemplate.exchange(
					baseUrl(cfg) + "/pg/orders",
					HttpMethod.POST,
					new HttpEntity<>(body.toString(), headers(cfg)),
					String.class);

			JSONObject json = new JSONObject(resp.getBody() == null ? "{}" : resp.getBody());
			String paymentSessionId = json.optString("payment_session_id", null);
			String cfOrderId = json.optString("order_id", orderId);
			if (paymentSessionId == null || paymentSessionId.isBlank()) {
				throw ApiException.server("Cashfree did not return a payment session");
			}
			return new CashfreeOrder(cfOrderId, paymentSessionId);
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			log.error("Failed to create Cashfree order", e);
			throw ApiException.server("Failed to create payment order: " + e.getMessage());
		}
	}

	/**
	 * Authoritative server-side verification: fetch the order's payments and
	 * return true if any one succeeded.
	 */
	public boolean verifyPayment(String cfOrderId) {
		if (cfOrderId == null || cfOrderId.isBlank()) {
			return false;
		}
		PaymentGatewayConfig cfg = requireEnabledConfig();
		try {
			ResponseEntity<String> resp = restTemplate.exchange(
					baseUrl(cfg) + "/pg/orders/" + cfOrderId + "/payments",
					HttpMethod.GET,
					new HttpEntity<>(headers(cfg)),
					String.class);

			String bodyStr = resp.getBody();
			if (bodyStr == null || bodyStr.isBlank()) {
				return false;
			}
			JSONArray payments = new JSONArray(bodyStr);
			for (int i = 0; i < payments.length(); i++) {
				JSONObject p = payments.optJSONObject(i);
				if (p != null && "SUCCESS".equalsIgnoreCase(p.optString("payment_status"))) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			log.warn("Cashfree payment verification error for order {}: {}", cfOrderId, e.getMessage());
			return false;
		}
	}

	private static String sanitizeId(String raw) {
		String cleaned = raw == null ? "" : raw.replaceAll("[^A-Za-z0-9_]", "_");
		return cleaned.isBlank() ? "store_unknown" : cleaned;
	}

	/** Result of a Cashfree order creation. */
	public record CashfreeOrder(String orderId, String paymentSessionId) {}
}
