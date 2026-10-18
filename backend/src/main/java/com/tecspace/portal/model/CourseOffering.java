package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/** A course offered in a term (separate from the grade-entry Course class). */
@Data
@Document(collection = "course_offerings")
public class CourseOffering {
    @Id
    private String id;

    @Indexed(unique = true)
    private String code;          // e.g. CS101

    private String name;
    private String department;
    private int creditHours;
    private int capacity;         // seats
    private String instructorName;
    private String term;          // e.g. "Fall 2026"
    private List<String> prerequisites = new ArrayList<>();

    /** Only students whose registration was approved can read these. */
    private String syllabus = "";
    private String syllabusUrl = "";
}
