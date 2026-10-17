package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "submissions")
@CompoundIndex(name = "assignment_student_unique", def = "{'assignmentId': 1, 'studentId': 1}", unique = true)
public class Submission {
    @Id
    private String id;
    private String assignmentId;
    private String studentId;
    private String text = "";
    private String link = "";
    private Instant submittedAt = Instant.now();
    private boolean late;
    private Double marks;          // null until graded
    private String feedback = "";
    private Instant gradedAt;
    private String gradedBy;
}
