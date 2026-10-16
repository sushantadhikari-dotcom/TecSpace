package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "timetable_slots")
public class TimetableSlot {
    @Id
    private String id;
    private String courseCode;
    private String day;        // MONDAY ... SUNDAY
    private String startTime;  // "09:00"
    private String endTime;    // "10:30"
    private String room;
}
