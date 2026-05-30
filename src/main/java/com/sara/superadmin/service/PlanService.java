package com.sara.superadmin.service;

import com.sara.superadmin.dto.PlanDto;
import com.sara.superadmin.dto.UpdatePlansRequest;
import com.sara.superadmin.model.Plan;
import com.sara.superadmin.model.PlanDuration;
import com.sara.superadmin.repository.PlanRepository;
import com.sara.superadmin.web.ApiException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class PlanService {

	private final PlanRepository repository;

	public PlanService(PlanRepository repository) {
		this.repository = repository;
	}

	/** The full pricing matrix, ordered by duration then service count. */
	public List<PlanDto> listPlans() {
		return repository.findAll().stream()
				.sorted(Comparator
						.comparing((Plan p) -> p.getDuration().ordinal())
						.thenComparingInt(Plan::getServiceCount))
				.map(PlanDto::from)
				.toList();
	}

	/** Resolve the price for a given duration + number of selected services. */
	public Plan resolve(PlanDuration duration, int serviceCount) {
		return repository.findByDurationAndServiceCount(duration, serviceCount)
				.filter(Plan::isActive)
				.orElseThrow(() -> ApiException.badRequest(
						"No active plan for " + duration + " with " + serviceCount + " services"));
	}

	/** Bulk update prices from the super-admin Plans form. Unknown rows are ignored. */
	public List<PlanDto> updatePlans(UpdatePlansRequest request) {
		for (UpdatePlansRequest.PlanPrice pp : request.plans()) {
			repository.findByDurationAndServiceCount(pp.duration(), pp.serviceCount())
					.ifPresent(plan -> {
						plan.setPrice(pp.price());
						if (pp.active() != null) {
							plan.setActive(pp.active());
						}
						repository.save(plan);
					});
		}
		return listPlans();
	}
}
