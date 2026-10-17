package com.tecspace.portal.dto;

import com.tecspace.portal.model.User;

import java.time.Instant;
import java.util.List;

public record UserView(String id, String universityEmail, String firstName, String lastName, String mobileNumber,
                       String role, List<String> permissions, boolean active, Instant createdAt) {
    public static UserView from(User u) {
        return new UserView(u.getId(), u.getUniversityEmail(), u.getFirstName(), u.getLastName(), u.getMobileNumber(),
                u.getRole(), u.getPermissions(), u.isActive(), u.getCreatedAt());
    }
}
