package com.tecspace.portal.repository;

import com.tecspace.portal.model.Scholarship;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ScholarshipRepository extends MongoRepository<Scholarship, String> {
    List<Scholarship> findByStudentIdOrderByAwardedAtDesc(String studentId);
    List<Scholarship> findAllByOrderByAwardedAtDesc();
}
