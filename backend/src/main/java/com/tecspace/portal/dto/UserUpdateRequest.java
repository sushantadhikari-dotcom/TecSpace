package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record UserUpdateRequest(
        @NotBlank @Pattern(regexp = "STAFF|TEACHER|ADMIN", message = "must be STAFF, TEACHER or ADMIN") String role,
        List<String> permissions,
        boolean active
) {}
