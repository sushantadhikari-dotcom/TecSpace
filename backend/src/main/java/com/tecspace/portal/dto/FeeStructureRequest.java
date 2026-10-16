package com.tecspace.portal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record FeeStructureRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "ALL|BSIT|BBA|MSCIT", message = "must be ALL, BSIT, BBA or MSCIT") String faculty,
        @NotEmpty @Valid List<FeeItemRequest> items
) {}
