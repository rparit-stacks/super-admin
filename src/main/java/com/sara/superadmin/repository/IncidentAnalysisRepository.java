package com.sara.superadmin.repository;

import com.sara.superadmin.model.IncidentAnalysis;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentAnalysisRepository extends MongoRepository<IncidentAnalysis, String> {

	Optional<IncidentAnalysis> findFirstByIncidentIdOrderByCreatedAtDesc(String incidentId);

	/** Past analyses for the same error signature — context for new analyses. */
	List<IncidentAnalysis> findByErrorSignatureOrderByCreatedAtDesc(String errorSignature, Pageable pageable);
}
