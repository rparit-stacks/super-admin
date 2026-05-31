package com.sara.superadmin.service;

import com.sara.superadmin.model.Store;
import com.sara.superadmin.repository.StoreRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifies a manually-registered store by calling its connector handshake
 * ({@code GET {apiBaseUrl}/api/admin/connector/handshake} with X-Store-Api-Key). On
 * success, records connected=true and the store's reported installedPlugins/version —
 * which then drive the UI's per-store services (no hardcode).
 */
@Service
public class ConnectorVerifyService {

	private static final Logger log = LoggerFactory.getLogger(ConnectorVerifyService.class);
	private static final String HANDSHAKE_PATH = "/api/admin/connector/handshake";

	private final StoreRepository storeRepository;
	private final RestTemplate restTemplate;

	public ConnectorVerifyService(StoreRepository storeRepository, RestTemplate livenessRestTemplate) {
		this.storeRepository = storeRepository;
		this.restTemplate = livenessRestTemplate;
	}

	public Store verify(Store store) {
		boolean ok = false;
		List<String> plugins = new ArrayList<>();
		String version = null;
		try {
			String base = store.getApiBaseUrl();
			if (base != null && base.endsWith("/")) {
				base = base.substring(0, base.length() - 1);
			}
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-Store-Api-Key", store.getApiKey());

			ResponseEntity<String> resp = restTemplate.exchange(
					base + HANDSHAKE_PATH, HttpMethod.GET, new HttpEntity<>(headers), String.class);

			if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
				JSONObject json = new JSONObject(resp.getBody());
				ok = json.optBoolean("ok", true);
				version = json.optString("version", null);
				JSONArray arr = json.optJSONArray("installedPlugins");
				if (arr != null) {
					for (int i = 0; i < arr.length(); i++) {
						plugins.add(arr.getString(i));
					}
				}
			}
		} catch (Exception e) {
			log.warn("Connector handshake failed for store {} ({}): {}",
					store.getName(), store.getApiBaseUrl(), e.getMessage());
			ok = false;
		}

		store.setConnected(ok);
		store.setLastHandshakeAt(Instant.now());
		if (ok) {
			store.setInstalledPlugins(plugins);
			store.setConnectorVersion(version);
			// The store dictates its services via installed plugins.
			store.setEnabledServices(plugins);
		}
		store.setUpdatedAt(Instant.now());
		return storeRepository.save(store);
	}
}
