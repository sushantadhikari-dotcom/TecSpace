package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;

public record TermActionRequest(@NotBlank String trimesterName) {}
