package com.tecspace.portal.dto;

import com.tecspace.portal.model.User;

import java.time.Instant;

public record StudentSummary(
        String id,
        String universityEmail,
        String firstName,
        String lastName,
        String mobileNumber,
        String faculty,
        String session,
        Instant createdAt
) {
    public static StudentSummary from(User u) {
        return new StudentSummary(u.getId(), u.getUniversityEmail(), u.getFirstName(), u.getLastName(),
                u.getMobileNumber(), u.getFaculty(), u.getSession(), u.getCreatedAt());
    }
}
