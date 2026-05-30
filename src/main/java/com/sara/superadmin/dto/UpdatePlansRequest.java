package com.sara.superadmin.dto;

import com.sara.superadmin.model.PlanDuration;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bulk update of the pricing matrix from the super-admin Plans form.
 * Each item is matched to an existing Plan row by (duration, serviceCount).
 */
public record UpdatePlansRequest(
		@NotNull List<PlanPrice> plans
) {
	public record PlanPrice(
			@NotNull PlanDuration duration,
			int serviceCount,
			@NotNull BigDecimal price,
			Boolean active
	) {}
}
