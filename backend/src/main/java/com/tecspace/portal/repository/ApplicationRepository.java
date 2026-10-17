package com.tecspace.portal.repository;

import com.tecspace.portal.model.Application;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends MongoRepository<Application, String> {
    Optional<Application> findByTrackingCode(String trackingCode);
    List<Application> findAllByOrderBySubmittedAtDesc();
    List<Application> findByStatusOrderBySubmittedAtDesc(String status);
    List<Application> findByFacultyAndSessionAndStatusOrderBySubmittedAtAsc(String faculty, String session, String status);
    long countByFacultyAndSessionAndStatusIn(String faculty, String session, Collection<String> statuses);
    boolean existsByEmailAndFacultyAndSessionAndStatusIn(String email, String faculty, String session, Collection<String> statuses);
}
