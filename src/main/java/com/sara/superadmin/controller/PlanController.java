package com.sara.superadmin.controller;

import com.sara.superadmin.dto.PlanDto;
import com.sara.superadmin.dto.UpdatePlansRequest;
import com.sara.superadmin.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Super-admin pricing-matrix view and edit. */
@RestController
@RequestMapping("/api/super/plans")
public class PlanController {

	private final PlanService planService;

	public PlanController(PlanService planService) {
		this.planService = planService;
	}

	@GetMapping
	public List<PlanDto> list() {
		return planService.listPlans();
	}

	@PutMapping
	public List<PlanDto> update(@Valid @RequestBody UpdatePlansRequest request) {
		return planService.updatePlans(request);
	}
}
