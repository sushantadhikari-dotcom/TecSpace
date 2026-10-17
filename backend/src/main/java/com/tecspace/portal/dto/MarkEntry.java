package com.tecspace.portal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MarkEntry(@NotBlank String studentId, @NotNull @DecimalMin("0") @DecimalMax("100") Double percentage) {}
