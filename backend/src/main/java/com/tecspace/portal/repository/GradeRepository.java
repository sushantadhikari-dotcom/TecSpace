package com.tecspace.portal.repository;

import com.tecspace.portal.model.GradeRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface GradeRepository extends MongoRepository<GradeRecord, String> {
    Optional<GradeRecord> findByStudentId(String studentId);
}
