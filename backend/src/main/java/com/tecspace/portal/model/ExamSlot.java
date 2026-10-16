package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "exam_slots")
public class ExamSlot {
    @Id
    private String id;
    private String courseCode;
    private LocalDate date;
    private String startTime;
    private String endTime;
    private String room;
    private String note;
}
