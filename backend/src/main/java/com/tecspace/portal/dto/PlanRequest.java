package com.tecspace.portal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PlanRequest(
        @NotBlank String studentId,
        @Min(2) @Max(12) int count,
        @NotNull LocalDate firstDueDate,
        @Min(7) @Max(180) int intervalDays
) {}
