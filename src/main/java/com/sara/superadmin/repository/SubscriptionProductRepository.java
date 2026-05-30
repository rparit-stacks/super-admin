package com.sara.superadmin.repository;

import com.sara.superadmin.model.SubscriptionProduct;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionProductRepository extends MongoRepository<SubscriptionProduct, String> {

	List<SubscriptionProduct> findByVisibleToStoresTrueOrderBySortOrderAsc();

	Optional<SubscriptionProduct> findByCode(String code);
}
