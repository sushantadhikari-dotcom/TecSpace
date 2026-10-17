package com.tecspace.portal.dto;

import jakarta.validation.constraints.*;

public record ApplyRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "must be a valid phone number") String mobileNumber,
        @NotBlank @Pattern(regexp = "BSIT|BBA|MSCIT", message = "must be BSIT, BBA or MSCIT") String faculty,
        @NotBlank @Pattern(regexp = "SPRING|FALL|SUMMER", message = "must be SPRING, FALL or SUMMER") String session,
        @NotBlank String previousSchool,
        @NotNull @DecimalMin("0") @DecimalMax("100") Double lastScore,
        String statement
) {}
