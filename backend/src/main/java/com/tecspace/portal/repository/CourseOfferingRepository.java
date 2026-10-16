package com.tecspace.portal.repository;

import com.tecspace.portal.model.CourseOffering;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CourseOfferingRepository extends MongoRepository<CourseOffering, String> {
    Optional<CourseOffering> findByCode(String code);
    boolean existsByCode(String code);
    List<CourseOffering> findByTerm(String term);
}
