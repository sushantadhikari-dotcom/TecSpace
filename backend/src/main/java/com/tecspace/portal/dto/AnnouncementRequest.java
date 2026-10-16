package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;

public record AnnouncementRequest(@NotBlank String title, @NotBlank String message) {}
