package com.sara.superadmin.service;

import com.sara.superadmin.dto.SubscriptionProductDto;
import com.sara.superadmin.repository.SubscriptionProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionProductService {

	private final SubscriptionProductRepository repository;

	public SubscriptionProductService(SubscriptionProductRepository repository) {
		this.repository = repository;
	}

	public List<SubscriptionProductDto> listVisibleForStore() {
		return repository.findByVisibleToStoresTrueOrderBySortOrderAsc().stream()
				.map(SubscriptionProductDto::from)
				.toList();
	}
}
