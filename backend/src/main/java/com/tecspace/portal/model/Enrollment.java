package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "enrollments")
public class Enrollment {
    public static final String ENROLLED = "ENROLLED";
    public static final String DROPPED = "DROPPED";

    @Id
    private String id;
    private String studentId;
    private String courseCode;
    private String term;
    private String status = ENROLLED;
    private Instant enrolledAt = Instant.now();
}
