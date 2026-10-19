package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CalendarRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Pattern(regexp = "REGISTRATION|CLASSES|EXAM|HOLIDAY|DEADLINE|OTHER", message = "is not a valid event type") String type,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 500) String note
) {}
