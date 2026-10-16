package com.tecspace.portal.repository;

import com.tecspace.portal.model.FeeStructure;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FeeStructureRepository extends MongoRepository<FeeStructure, String> {
}
