package com.tecspace.portal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String universityEmail;

    @JsonIgnore
    private String password;

    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String faculty;   // BSIT, BBA, MSCIT
    private String session;   // SPRING, FALL, SUMMER
    private String role;      // ROLE_STUDENT, ROLE_ADMIN
    private Instant createdAt = Instant.now();
}
