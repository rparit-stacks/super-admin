package com.sara.superadmin.dto;

import com.sara.superadmin.model.SubscriptionProduct;

import java.math.BigDecimal;

/** Store-facing subscription picker row. */
public record SubscriptionProductDto(
		String code,
		String title,
		String description,
		int sortOrder,
		String checkoutFlow,
		BigDecimal monthlyPrice
) {
	public static SubscriptionProductDto from(SubscriptionProduct p) {
		return new SubscriptionProductDto(
				p.getCode(),
				p.getTitle(),
				p.getDescription(),
				p.getSortOrder(),
				p.getCheckoutFlow(),
				p.getMonthlyPrice());
	}
}
