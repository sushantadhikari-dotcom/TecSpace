package com.tecspace.portal.repository;

import com.tecspace.portal.model.TimetableSlot;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface TimetableSlotRepository extends MongoRepository<TimetableSlot, String> {
    List<TimetableSlot> findByDay(String day);
    List<TimetableSlot> findByCourseCode(String courseCode);
    List<TimetableSlot> findByCourseCodeIn(Collection<String> courseCodes);
    void deleteByCourseCode(String courseCode);
}
