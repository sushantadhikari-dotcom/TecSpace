package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "applications")
public class Application {
    public static final String SUBMITTED = "SUBMITTED";
    public static final String APPROVED = "APPROVED";
    public static final String WAITLISTED = "WAITLISTED";
    public static final String REJECTED = "REJECTED";
    public static final String ENROLLED = "ENROLLED";

    @Id
    private String id;

    @Indexed(unique = true)
    private String trackingCode;

    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String faculty;
    private String session;
    private String previousSchool;
    private Double lastScore;
    private String statement;

    private List<ApplicationDocument> documents = new ArrayList<>();
    private String status = SUBMITTED;
    private String remarks = "";
    private String decidedBy;
    private Instant submittedAt = Instant.now();
    private Instant decidedAt;
    private String studentId;     // set once the applicant is enrolled
}
