package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "calendar_events")
public class CalendarEvent {
    @Id
    private String id;
    private String title;
    private String type;        // REGISTRATION, CLASSES, EXAM, HOLIDAY, DEADLINE, OTHER
    private LocalDate startDate;
    private LocalDate endDate;
    private String note = "";
}
