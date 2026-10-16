package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;

public record ContactAdminRequest(@NotBlank String subject, @NotBlank String body) {}
