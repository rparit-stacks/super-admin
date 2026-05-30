package com.sara.superadmin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/**
 * Pricing matrix row. The price depends on BOTH the duration and the number of
 * gateways (services) the store selects. Min selectable services is 2.
 *
 * Default seeded matrix (INR):
 *                 2 svc   3 svc   4 svc
 *   6 Months      3299    3699    3999
 *   1 Year        4299    4699    4999
 *   Lifetime      5299    5699    5999
 *
 * Editable by super-admin via the Plans form.
 */
@Document(collection = "plans")
@CompoundIndex(name = "duration_servicecount_unique", def = "{'duration': 1, 'serviceCount': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

	@Id
	private String id;

	private PlanDuration duration;

	/** Number of gateways included (2, 3 or 4). */
	private int serviceCount;

	private BigDecimal price;

	@Builder.Default
	private String currency = "INR";

	/** Allow super-admin to retire a row without deleting it. */
	@Builder.Default
	private boolean active = true;
}
