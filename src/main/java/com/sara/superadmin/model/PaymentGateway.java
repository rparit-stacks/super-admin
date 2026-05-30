package com.sara.superadmin.model;

/**
 * The payment gateways a store can subscribe to / unlock.
 * These map 1:1 to the store backend's configurable gateways.
 * PAYU is offered in plans but not yet implemented on the store side (config-only for now).
 */
public enum PaymentGateway {
	RAZORPAY,
	STRIPE,
	PARTIAL_COD,
	PAYU
}
