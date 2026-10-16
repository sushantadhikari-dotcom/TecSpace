package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;

public record SlotRequest(
        @NotBlank String courseCode,
        @NotBlank String day,
        @NotBlank String startTime,
        @NotBlank String endTime,
        @NotBlank String room
) {}
