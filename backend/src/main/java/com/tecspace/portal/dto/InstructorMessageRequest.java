package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;

public record InstructorMessageRequest(@NotBlank String courseCode, @NotBlank String subject, @NotBlank String body) {}
