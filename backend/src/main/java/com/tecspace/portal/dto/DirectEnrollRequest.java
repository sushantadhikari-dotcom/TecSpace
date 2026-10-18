package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;

public record DirectEnrollRequest(@NotBlank String studentId, @NotBlank String courseCode, boolean overridePrerequisites) {}
