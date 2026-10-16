package com.tecspace.portal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ScholarshipRequest(
        @NotBlank String studentId,
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "PERCENT|FIXED", message = "must be PERCENT or FIXED") String type,
        @NotNull @DecimalMin("0.01") BigDecimal value
) {}
