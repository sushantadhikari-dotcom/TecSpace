package com.tecspace.portal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CourseRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String department,
        @Min(1) int creditHours,
        @Min(1) int capacity,
        String instructorName,
        List<String> prerequisites
) {}
