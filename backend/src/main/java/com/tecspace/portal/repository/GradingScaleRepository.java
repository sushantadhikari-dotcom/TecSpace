package com.tecspace.portal.repository;

import com.tecspace.portal.model.GradingScale;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GradingScaleRepository extends MongoRepository<GradingScale, String> {
}
