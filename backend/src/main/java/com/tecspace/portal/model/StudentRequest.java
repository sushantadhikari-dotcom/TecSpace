package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Document(collection = "student_requests")
public class StudentRequest {
    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    @Id
    private String id;

    @Indexed
    private String studentId;

    private String type;        // TRANSCRIPT, ID_REISSUE, BONAFIDE, LEAVE
    private String details;     // purpose or reason
    private LocalDate leaveFrom;
    private LocalDate leaveTo;
    private String status = PENDING;
    private String adminNote;
    private String decidedBy;
    private Instant createdAt = Instant.now();
    private Instant decidedAt;
}
