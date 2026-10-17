package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DocUpdate(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "PENDING|VERIFIED|REJECTED", message = "must be PENDING, VERIFIED or REJECTED") String status,
        String note
) {}
