package com.tecspace.portal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SeatConfigRequest(
        @NotBlank @Pattern(regexp = "BSIT|BBA|MSCIT") String faculty,
        @NotBlank @Pattern(regexp = "SPRING|FALL|SUMMER") String session,
        @Min(0) int seats
) {}
