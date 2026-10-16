package com.tecspace.portal.repository;

import com.tecspace.portal.model.AttendanceRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends MongoRepository<AttendanceRecord, String> {
    Optional<AttendanceRecord> findByCourseCodeAndDate(String courseCode, LocalDate date);
    List<AttendanceRecord> findByCourseCodeAndTerm(String courseCode, String term);
}
