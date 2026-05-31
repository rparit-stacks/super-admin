package com.sara.superadmin.controller;

import com.sara.superadmin.ai.AiProvider;
import com.sara.superadmin.ai.AiProviderSelector;
import com.sara.superadmin.dto.AiProviderConfigDto;
import com.sara.superadmin.model.Incident;
import com.sara.superadmin.model.IncidentAnalysis;
import com.sara.superadmin.repository.IncidentAnalysisRepository;
import com.sara.superadmin.repository.IncidentRepository;
import com.sara.superadmin.service.AiAnalysisService;
import com.sara.superadmin.service.AiProviderConfigService;
import com.sara.superadmin.web.ApiException;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Super-admin RCA management: AI provider config + incident/analysis views. */
@RestController
@RequestMapping("/api/super/rca")
public class RcaAdminController {

	private final AiProviderConfigService configService;
	private final IncidentRepository incidentRepository;
	private final IncidentAnalysisRepository analysisRepository;
	private final AiAnalysisService analysisService;
	private final AiProviderSelector selector;

	public RcaAdminController(AiProviderConfigService configService,
							  IncidentRepository incidentRepository,
							  IncidentAnalysisRepository analysisRepository,
							  AiAnalysisService analysisService,
							  AiProviderSelector selector) {
		this.configService = configService;
		this.incidentRepository = incidentRepository;
		this.analysisRepository = analysisRepository;
		this.analysisService = analysisService;
		this.selector = selector;
	}

	@GetMapping("/ai-provider-config")
	public AiProviderConfigDto getConfig() {
		return configService.getConfigMasked();
	}

	@PutMapping("/ai-provider-config")
	public AiProviderConfigDto updateConfig(@Valid @RequestBody AiProviderConfigDto dto) {
		return configService.updateConfig(dto);
	}

	/** Recent incidents, optionally filtered by store. */
	@GetMapping("/incidents")
	public List<Incident> incidents(@RequestParam(required = false) String storeId,
									@RequestParam(defaultValue = "50") int limit) {
		PageRequest page = PageRequest.of(0, Math.min(limit, 200));
		if (storeId != null && !storeId.isBlank()) {
			return incidentRepository.findByStoreIdOrderByCreatedAtDesc(storeId, page);
		}
		return incidentRepository.findAll(
				PageRequest.of(0, Math.min(limit, 200),
						org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
				.getContent();
	}

	/** One incident + its latest analysis. */
	@GetMapping("/incidents/{id}")
	public Map<String, Object> incident(@PathVariable String id) {
		Incident incident = incidentRepository.findById(id)
				.orElseThrow(() -> ApiException.notFound("Incident not found"));
		IncidentAnalysis analysis = analysisRepository
				.findFirstByIncidentIdOrderByCreatedAtDesc(id).orElse(null);
		return Map.of("incident", incident, "analysis", analysis == null ? Map.of() : analysis);
	}

	/** Manually (re)run analysis for one incident using the active provider. */
	@PostMapping("/incidents/{id}/reanalyze")
	public IncidentAnalysis reanalyze(@PathVariable String id) {
		Incident incident = incidentRepository.findById(id)
				.orElseThrow(() -> ApiException.notFound("Incident not found"));
		AiProvider provider = selector.active()
				.orElseThrow(() -> ApiException.badRequest("No active/enabled AI provider configured"));
		return analysisService.analyzeOne(incident, provider);
	}
}
