package com.sara.superadmin.dto;

import com.sara.superadmin.model.Plan;
import com.sara.superadmin.model.PlanDuration;

import java.math.BigDecimal;

/** A single pricing-matrix cell, used both for reading and updating. */
public record PlanDto(
		String id,
		PlanDuration duration,
		int serviceCount,
		BigDecimal price,
		String currency,
		boolean active
) {
	public static PlanDto from(Plan p) {
		return new PlanDto(p.getId(), p.getDuration(), p.getServiceCount(),
				p.getPrice(), p.getCurrency(), p.isActive());
	}
}
