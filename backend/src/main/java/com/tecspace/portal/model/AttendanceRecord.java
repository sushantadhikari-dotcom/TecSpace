package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** One register: one course on one day. */
@Data
@Document(collection = "attendance")
@CompoundIndex(name = "course_date_unique", def = "{'courseCode': 1, 'date': 1}", unique = true)
public class AttendanceRecord {
    @Id
    private String id;
    private String courseCode;
    private String term;
    private LocalDate date;
    private List<AttendanceEntry> entries = new ArrayList<>();
}
