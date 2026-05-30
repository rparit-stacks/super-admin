package com.sara.superadmin.repository;

import com.sara.superadmin.model.SubscriptionProduct;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SubscriptionProductRepository extends MongoRepository<SubscriptionProduct, String> {

	List<SubscriptionProduct> findByVisibleToStoresTrueOrderBySortOrderAsc();
}
