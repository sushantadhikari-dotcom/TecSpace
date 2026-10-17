package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Document(collection = "assignments")
public class Assignment {
    @Id
    private String id;

    @Indexed
    private String courseCode;

    private String title;
    private String description = "";
    private LocalDateTime dueAt;
    private double maxMarks;
    private String createdBy;      // teacher id
    private Instant createdAt = Instant.now();
}
