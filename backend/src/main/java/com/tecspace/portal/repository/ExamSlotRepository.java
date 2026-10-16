package com.tecspace.portal.repository;

import com.tecspace.portal.model.ExamSlot;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ExamSlotRepository extends MongoRepository<ExamSlot, String> {
    List<ExamSlot> findByDate(LocalDate date);
    List<ExamSlot> findByCourseCodeIn(Collection<String> courseCodes);
}
