package com.tecspace.portal.repository;

import com.tecspace.portal.model.StudentRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StudentRequestRepository extends MongoRepository<StudentRequest, String> {
    List<StudentRequest> findByStudentIdOrderByCreatedAtDesc(String studentId);
    List<StudentRequest> findAllByOrderByCreatedAtDesc();
    List<StudentRequest> findByStatusOrderByCreatedAtDesc(String status);
    boolean existsByStudentIdAndTypeAndStatus(String studentId, String type, String status);
}
