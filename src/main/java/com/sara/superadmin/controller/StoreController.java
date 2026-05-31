package com.sara.superadmin.controller;

import com.sara.superadmin.dto.LivenessResult;
import com.sara.superadmin.dto.MaintenanceWindowRequest;
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
	private final com.sara.superadmin.service.ConnectorVerifyService connectorVerifyService;

	public StoreController(StoreService storeService,
						   com.sara.superadmin.service.ConnectorVerifyService connectorVerifyService) {
		this.storeService = storeService;
		this.connectorVerifyService = connectorVerifyService;
	}

	@GetMapping
	public List<StoreResponse> list() {
		return storeService.listStores();
	}

	@PostMapping
	public StoreResponse create(@Valid @RequestBody StoreRequest req) {
		StoreResponse created = storeService.createStore(req);
		// Try to verify the connector immediately so the UI shows connected + plugins.
		try {
			connectorVerifyService.verify(storeService.getStoreOrThrow(created.id()));
		} catch (Exception ignored) {
			// Non-fatal; admin can hit "Connect" later.
		}
		return storeService.getStore(created.id());
	}

	/** Verify/connect a store by calling its connector handshake; updates connected + installedPlugins. */
	@PostMapping("/{id}/connect")
	public StoreResponse connect(@PathVariable String id) {
		return StoreResponse.from(connectorVerifyService.verify(storeService.getStoreOrThrow(id)));
	}

	@GetMapping("/{id}")
	public StoreResponse get(@PathVariable String id) {
		return storeService.getStore(id);
	}

	@PutMapping("/{id}")
	public StoreResponse update(@PathVariable String id, @Valid @RequestBody StoreRequest req) {
		return storeService.updateStore(id, req);
	}

	/**
	 * Set or clear ONLY the complimentary maintenance window. Kept separate from the
	 * general store update so saving the date can never wipe other fields, and a general
	 * edit can never wipe the date.
	 */
	@PutMapping("/{id}/maintenance-window")
	public StoreResponse setMaintenanceWindow(@PathVariable String id,
											  @RequestBody MaintenanceWindowRequest req) {
		return storeService.setMaintenanceWindow(id, req.maintenanceFreeUntil());
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
