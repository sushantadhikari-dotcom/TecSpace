package com.tecspace.portal.dto;

import com.tecspace.portal.model.GradeBand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GradingScaleRequest(@NotBlank String name, @NotEmpty List<GradeBand> bands) {}
