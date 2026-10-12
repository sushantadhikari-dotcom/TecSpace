package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String universityEmail, @NotBlank String password) {}
