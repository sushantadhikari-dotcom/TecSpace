package com.tecspace.portal.repository;

import com.tecspace.portal.model.FacultyAssignment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FacultyAssignmentRepository extends MongoRepository<FacultyAssignment, String> {
    List<FacultyAssignment> findByTeacherId(String teacherId);
    List<FacultyAssignment> findByCourseCode(String courseCode);
    void deleteByTeacherId(String teacherId);
}
