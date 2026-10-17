package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;

public record TeacherMessageRequest(@NotBlank String courseCode, @NotBlank String subject, @NotBlank String body) {}
