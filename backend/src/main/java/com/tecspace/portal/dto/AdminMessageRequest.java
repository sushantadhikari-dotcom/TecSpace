package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminMessageRequest(
        @NotBlank @Pattern(regexp = "ALL|COURSE|STUDENT", message = "must be ALL, COURSE or STUDENT") String target,
        String courseCode,
        String studentId,
        @NotBlank String subject,
        @NotBlank String body
) {}
