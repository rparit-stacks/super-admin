package com.sara.superadmin.controller;

import com.sara.superadmin.dto.LivenessResult;
import com.sara.superadmin.dto.StoreDetailResponse;
import com.sara.superadmin.dto.StoreRequest;
import com.sara.superadmin.dto.StoreResponse;
import com.sara.superadmin.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Super-admin store registry: list, register, go-into-store detail, liveness. */
@RestController
@RequestMapping("/api/super/stores")
public class StoreController {

	private final StoreService storeService;

	public StoreController(StoreService storeService) {
		this.storeService = storeService;
	}

	@GetMapping
	public List<StoreResponse> list() {
		return storeService.listStores();
	}

	@PostMapping
	public StoreResponse create(@Valid @RequestBody StoreRequest req) {
		return storeService.createStore(req);
	}

	@GetMapping("/{id}")
	public StoreResponse get(@PathVariable String id) {
		return storeService.getStore(id);
	}

	@PutMapping("/{id}")
	public StoreResponse update(@PathVariable String id, @Valid @RequestBody StoreRequest req) {
		return storeService.updateStore(id, req);
	}

	/** Full detail when the super-admin "goes into" a store. */
	@GetMapping("/{id}/detail")
	public StoreDetailResponse detail(@PathVariable String id) {
		return storeService.getStoreDetail(id);
	}

	/** On-demand liveness check; also persists the latest status. */
	@GetMapping("/{id}/status")
	public LivenessResult status(@PathVariable String id) {
		return storeService.checkLiveness(id);
	}
}
