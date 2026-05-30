package com.sara.superadmin.service;

import com.sara.superadmin.dto.LivenessResult;
import com.sara.superadmin.dto.StoreDetailResponse;
import com.sara.superadmin.dto.StoreRequest;
import com.sara.superadmin.dto.StoreResponse;
import com.sara.superadmin.dto.SubscriptionDto;
import com.sara.superadmin.model.Store;
import com.sara.superadmin.model.StoreStatus;
import com.sara.superadmin.model.Subscription;
import com.sara.superadmin.repository.StoreRepository;
import com.sara.superadmin.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class StoreService {

	private static final Logger log = LoggerFactory.getLogger(StoreService.class);

	private final StoreRepository storeRepository;
	private final SubscriptionService subscriptionService;
	private final RestTemplate restTemplate;
	private final String healthPath;

	public StoreService(StoreRepository storeRepository,
						SubscriptionService subscriptionService,
						RestTemplate livenessRestTemplate,
						@Value("${super-admin.liveness.health-path}") String healthPath) {
		this.storeRepository = storeRepository;
		this.subscriptionService = subscriptionService;
		this.restTemplate = livenessRestTemplate;
		this.healthPath = healthPath;
	}

	public List<StoreResponse> listStores() {
		return storeRepository.findAll().stream().map(StoreResponse::from).toList();
	}

	public Store getStoreOrThrow(String id) {
		return storeRepository.findById(id)
				.orElseThrow(() -> ApiException.notFound("Store not found: " + id));
	}

	public StoreResponse getStore(String id) {
		return StoreResponse.from(getStoreOrThrow(id));
	}

	/** Full "go into the store" detail: store + active subscription + history. */
	public StoreDetailResponse getStoreDetail(String id) {
		Store store = getStoreOrThrow(id);
		Subscription activePayment = subscriptionService.findActiveSubscription(id).orElse(null);
		Subscription activeMaint = subscriptionService.findActiveMaintenanceSubscription(id).orElse(null);
		List<SubscriptionDto> history = subscriptionService.listForStore(id);
		return new StoreDetailResponse(
				StoreResponse.from(store),
				activePayment != null ? SubscriptionDto.from(activePayment) : null,
				activeMaint != null ? SubscriptionDto.from(activeMaint) : null,
				history);
	}

	public StoreResponse createStore(StoreRequest req) {
		if (storeRepository.existsByName(req.name())) {
			throw ApiException.badRequest("A store with this name already exists");
		}
		Instant now = Instant.now();
		Store store = Store.builder()
				.name(req.name())
				.code(req.code())
				.apiBaseUrl(stripTrailingSlash(req.apiBaseUrl()))
				.websiteUrl(req.websiteUrl())
				.apiKey(req.apiKey() != null && !req.apiKey().isBlank() ? req.apiKey() : generateApiKey())
				.status(StoreStatus.UNKNOWN)
				.enabled(req.enabled() == null || req.enabled())
				.contactEmail(req.contactEmail())
				.contactPhone(req.contactPhone())
				.notes(req.notes())
				.maintenanceFreeUntil(req.maintenanceFreeUntil())
				.createdAt(now)
				.updatedAt(now)
				.build();
		return StoreResponse.from(storeRepository.save(store));
	}

	public StoreResponse updateStore(String id, StoreRequest req) {
		Store store = getStoreOrThrow(id);
		store.setName(req.name());
		store.setCode(req.code());
		store.setApiBaseUrl(stripTrailingSlash(req.apiBaseUrl()));
		store.setWebsiteUrl(req.websiteUrl());
		if (req.apiKey() != null && !req.apiKey().isBlank()) {
			store.setApiKey(req.apiKey());
		}
		store.setContactEmail(req.contactEmail());
		store.setContactPhone(req.contactPhone());
		store.setNotes(req.notes());
		store.setMaintenanceFreeUntil(req.maintenanceFreeUntil());
		if (req.enabled() != null) {
			store.setEnabled(req.enabled());
		}
		store.setUpdatedAt(Instant.now());
		return StoreResponse.from(storeRepository.save(store));
	}

	// ---------------- Liveness ----------------

	/** Ping one store's health endpoint and persist the result. */
	public LivenessResult checkLiveness(String id) {
		Store store = getStoreOrThrow(id);
		return pingAndPersist(store);
	}

	private LivenessResult pingAndPersist(Store store) {
		String url = store.getApiBaseUrl() + healthPath;
		Instant checkedAt = Instant.now();
		long start = System.nanoTime();
		StoreStatus status;
		Integer httpStatus = null;
		Long latencyMs = null;
		String detail = null;
		try {
			ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
			latencyMs = (System.nanoTime() - start) / 1_000_000;
			httpStatus = resp.getStatusCode().value();
			status = resp.getStatusCode().is2xxSuccessful() ? StoreStatus.LIVE : StoreStatus.DOWN;
		} catch (Exception e) {
			status = StoreStatus.DOWN;
			detail = e.getClass().getSimpleName() + ": " + e.getMessage();
			log.debug("Liveness ping failed for store {} at {}: {}", store.getId(), url, detail);
		}

		store.setStatus(status);
		store.setLastPingAt(checkedAt);
		store.setLastPingLatencyMs(status == StoreStatus.LIVE ? latencyMs : null);
		store.setUpdatedAt(checkedAt);
		storeRepository.save(store);

		return new LivenessResult(store.getId(), status, latencyMs, httpStatus, checkedAt, detail);
	}

	/** Periodically refresh liveness of all enabled stores (every 60s). */
	@Scheduled(fixedDelayString = "${super-admin.liveness.interval-ms:60000}")
	public void refreshAllLiveness() {
		for (Store store : storeRepository.findAll()) {
			if (store.isEnabled()) {
				pingAndPersist(store);
			}
		}
	}

	private String generateApiKey() {
		return "sk_store_" + UUID.randomUUID().toString().replace("-", "");
	}

	private String stripTrailingSlash(String url) {
		if (url == null) return null;
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
