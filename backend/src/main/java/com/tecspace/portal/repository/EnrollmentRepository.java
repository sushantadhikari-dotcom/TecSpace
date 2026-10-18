package com.tecspace.portal.repository;

import com.tecspace.portal.model.Enrollment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {
    List<Enrollment> findByStudentIdAndTermAndStatus(String studentId, String term, String status);
    List<Enrollment> findByStudentIdAndTerm(String studentId, String term);
    List<Enrollment> findByCourseCodeAndTermAndStatus(String courseCode, String term, String status);
    List<Enrollment> findByStudentIdOrderByEnrolledAtDesc(String studentId);
    List<Enrollment> findByStatusOrderByEnrolledAtDesc(String status);
    List<Enrollment> findByStudentIdAndCourseCodeAndTermAndStatusIn(String studentId, String courseCode, String term, Collection<String> statuses);
    Optional<Enrollment> findByStudentIdAndCourseCodeAndTermAndStatus(String studentId, String courseCode, String term, String status);
    long countByCourseCodeAndTermAndStatus(String courseCode, String term, String status);
    long countByCourseCodeAndStatus(String courseCode, String status);
}
