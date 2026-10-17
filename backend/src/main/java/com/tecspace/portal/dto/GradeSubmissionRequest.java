package com.tecspace.portal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GradeSubmissionRequest(
        @NotBlank String studentId,
        @NotNull @DecimalMin("0") Double marks,
        @Size(max = 2000) String feedback
) {}
