package com.tecspace.portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email String universityEmail,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "must be a valid phone number") String mobileNumber,
        @NotBlank @Pattern(regexp = "BSIT|BBA|MSCIT", message = "must be BSIT, BBA or MSCIT") String faculty,
        @NotBlank @Pattern(regexp = "SPRING|FALL|SUMMER", message = "must be SPRING, FALL or SUMMER") String session
) {}
