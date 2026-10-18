package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A student's registration in a course. A student can only ask (PENDING).
 * The administration decides: ENROLLED (approved) or REJECTED. DROPPED = withdrawn or cancelled by the student.
 */
@Data
@Document(collection = "enrollments")
public class Enrollment {
    public static final String PENDING = "PENDING";
    public static final String ENROLLED = "ENROLLED";
    public static final String REJECTED = "REJECTED";
    public static final String DROPPED = "DROPPED";

    @Id
    private String id;
    private String studentId;
    private String courseCode;
    private String term;
    private String status = PENDING;
    private Instant enrolledAt = Instant.now();   // when it was requested (reset to the approval time when approved)
    private Instant decidedAt;
    private String decidedBy;
    private String note = "";
}
