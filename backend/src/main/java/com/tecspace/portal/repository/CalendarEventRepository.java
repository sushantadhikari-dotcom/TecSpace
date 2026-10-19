package com.tecspace.portal.repository;

import com.tecspace.portal.model.CalendarEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CalendarEventRepository extends MongoRepository<CalendarEvent, String> {
    List<CalendarEvent> findAllByOrderByStartDateAsc();
    List<CalendarEvent> findByType(String type);
}
