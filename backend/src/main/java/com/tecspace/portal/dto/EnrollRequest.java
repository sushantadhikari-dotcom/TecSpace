package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;

public record EnrollRequest(@NotBlank String courseCode) {}
