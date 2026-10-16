package com.tecspace.portal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record AttendanceSaveRequest(
        @NotBlank String courseCode,
        @NotNull LocalDate date,
        @NotNull @Valid List<AttendanceEntryRequest> entries
) {}
