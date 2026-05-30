package com.sara.superadmin.repository;

import com.sara.superadmin.model.SuperAdmin;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SuperAdminRepository extends MongoRepository<SuperAdmin, String> {
	Optional<SuperAdmin> findByUsername(String username);
	long count();
}
