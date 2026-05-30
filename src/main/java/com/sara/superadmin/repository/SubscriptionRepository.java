package com.sara.superadmin.repository;

import com.sara.superadmin.model.Subscription;
import com.sara.superadmin.model.SubscriptionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {
	List<Subscription> findByStoreIdOrderByCreatedAtDesc(String storeId);
	List<Subscription> findByStoreIdAndStatus(String storeId, SubscriptionStatus status);
	Optional<Subscription> findByRazorpayOrderId(String razorpayOrderId);
	List<Subscription> findByStatus(SubscriptionStatus status);
}
