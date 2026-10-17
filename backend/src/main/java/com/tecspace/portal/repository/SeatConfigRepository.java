package com.tecspace.portal.repository;

import com.tecspace.portal.model.SeatConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SeatConfigRepository extends MongoRepository<SeatConfig, String> {
    Optional<SeatConfig> findByFacultyAndSession(String faculty, String session);
}
