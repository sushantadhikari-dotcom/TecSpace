package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Personal details, emergency contacts and medical information of one student. */
@Data
@Document(collection = "student_profiles")
public class StudentProfile {
    @Id
    private String id;

    @Indexed(unique = true)
    private String studentId;

    private LocalDate dateOfBirth;
    private String gender = "";
    private String address = "";
    private String nationality = "";
    private List<EmergencyContact> emergencyContacts = new ArrayList<>();
    private MedicalInfo medical = new MedicalInfo();
    private Instant updatedAt = Instant.now();
    private String updatedBy;
}
