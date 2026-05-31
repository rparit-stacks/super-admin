package com.sara.superadmin.repository;

import com.sara.superadmin.model.Incident;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends MongoRepository<Incident, String> {

	Optional<Incident> findByStoreIdAndStoreApiLogId(String storeId, Long storeApiLogId);

	List<Incident> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

	List<Incident> findByStoreIdOrderByCreatedAtDesc(String storeId, Pageable pageable);
}
