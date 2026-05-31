package com.sara.superadmin.repository;

import com.sara.superadmin.model.AiProviderConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AiProviderConfigRepository extends MongoRepository<AiProviderConfig, String> {
	Optional<AiProviderConfig> findFirstByOrderByIdAsc();
}
