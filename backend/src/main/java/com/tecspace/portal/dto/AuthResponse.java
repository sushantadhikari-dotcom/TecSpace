package com.tecspace.portal.dto;

public record AuthResponse(
        String token,
        String id,
        String universityEmail,
        String firstName,
        String lastName,
        String faculty,
        String session,
        String role,
        boolean mustChangePassword
) {}
