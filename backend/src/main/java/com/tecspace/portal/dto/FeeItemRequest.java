package com.tecspace.portal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record FeeItemRequest(
        @NotBlank @Pattern(regexp = "TUITION|LAB|HOSTEL|OTHER", message = "must be TUITION, LAB, HOSTEL or OTHER") String category,
        @NotBlank String label,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        boolean optional
) {}
