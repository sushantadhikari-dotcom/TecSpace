package com.tecspace.portal.repository;

import com.tecspace.portal.model.FinancialRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FinancialRepository extends MongoRepository<FinancialRecord, String> {
    Optional<FinancialRecord> findByStudentId(String studentId);
}
