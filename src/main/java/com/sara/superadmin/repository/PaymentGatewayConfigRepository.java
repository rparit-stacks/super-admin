package com.sara.superadmin.repository;

import com.sara.superadmin.model.PaymentGatewayConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PaymentGatewayConfigRepository extends MongoRepository<PaymentGatewayConfig, String> {
	/** Singleton config: there is only ever one document. */
	Optional<PaymentGatewayConfig> findFirstByOrderByIdAsc();
}
