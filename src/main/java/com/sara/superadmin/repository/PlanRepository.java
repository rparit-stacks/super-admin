package com.sara.superadmin.repository;

import com.sara.superadmin.model.Plan;
import com.sara.superadmin.model.PlanDuration;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PlanRepository extends MongoRepository<Plan, String> {
	Optional<Plan> findByDurationAndServiceCount(PlanDuration duration, int serviceCount);
	long count();
}
