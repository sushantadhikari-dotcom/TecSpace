package com.tecspace.portal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AssignmentRequest(
        @NotBlank String courseCode,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 3000) String description,
        @NotNull LocalDateTime dueAt,
        @DecimalMin(value = "1", message = "must be at least 1") double maxMarks
) {}
