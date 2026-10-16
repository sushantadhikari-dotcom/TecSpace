package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExamRequest(
        @NotBlank String courseCode,
        @NotNull LocalDate date,
        @NotBlank String startTime,
        @NotBlank String endTime,
        @NotBlank String room,
        String note
) {}
