package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DecisionRequest(
        @NotBlank @Pattern(regexp = "APPROVE|REJECT", message = "must be APPROVE or REJECT") String decision,
        String remarks
) {}
