package com.sara.superadmin.service;

import com.sara.superadmin.dto.SubscriptionProductDto;
import com.sara.superadmin.model.Store;
import com.sara.superadmin.repository.SubscriptionProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class SubscriptionProductService {

	private final SubscriptionProductRepository repository;

	public SubscriptionProductService(SubscriptionProductRepository repository) {
		this.repository = repository;
	}

	/** All globally-visible products (unfiltered) — kept for any non-store callers. */
	public List<SubscriptionProductDto> listVisibleForStore() {
		return repository.findByVisibleToStoresTrueOrderBySortOrderAsc().stream()
				.map(SubscriptionProductDto::from)
				.toList();
	}

	/**
	 * Products this specific store offers: globally visible AND in the store's
	 * {@code enabledServices}. A store with null/empty services (legacy) sees all
	 * visible products, preserving old behaviour.
	 */
	public List<SubscriptionProductDto> listVisibleForStore(Store store) {
		List<String> enabled = store == null ? null : store.getEnabledServices();
		Set<String> allowed = (enabled == null || enabled.isEmpty()) ? null : Set.copyOf(enabled);
		return repository.findByVisibleToStoresTrueOrderBySortOrderAsc().stream()
				.filter(p -> allowed == null || (p.getCode() != null && allowed.contains(p.getCode())))
				.map(SubscriptionProductDto::from)
				.toList();
	}
}
