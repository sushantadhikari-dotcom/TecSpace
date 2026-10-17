package com.tecspace.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record UserCreateRequest(
        @NotBlank @Email String universityEmail,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "must be a valid phone number") String mobileNumber,
        @NotBlank @Pattern(regexp = "STAFF|TEACHER|ADMIN", message = "must be STAFF, TEACHER or ADMIN") String role,
        List<String> permissions
) {}
