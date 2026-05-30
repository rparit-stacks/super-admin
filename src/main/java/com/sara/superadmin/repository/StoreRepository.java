package com.sara.superadmin.repository;

import com.sara.superadmin.model.Store;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface StoreRepository extends MongoRepository<Store, String> {
	Optional<Store> findByName(String name);
	Optional<Store> findByApiKey(String apiKey);
	boolean existsByName(String name);
}
