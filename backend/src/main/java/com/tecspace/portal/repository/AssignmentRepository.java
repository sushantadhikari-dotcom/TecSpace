package com.tecspace.portal.repository;

import com.tecspace.portal.model.Assignment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends MongoRepository<Assignment, String> {
    List<Assignment> findByCourseCodeIn(Collection<String> courseCodes);
    List<Assignment> findByCourseCode(String courseCode);
}
