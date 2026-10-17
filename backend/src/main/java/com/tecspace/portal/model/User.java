package com.tecspace.portal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    private String faculty;   // BSIT, BBA, MSCIT (students)
    private String session;   // SPRING, FALL, SUMMER (students)
    private String role;      // ROLE_STUDENT, ROLE_ADMIN, ROLE_STAFF, ROLE_TEACHER
    private List<String> permissions = new ArrayList<>();   // used by ROLE_STAFF
    private boolean active = true;                           // false = account disabled
    private Instant createdAt = Instant.now();
}
