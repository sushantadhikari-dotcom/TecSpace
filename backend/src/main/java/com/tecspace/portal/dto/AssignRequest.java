package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignRequest(@NotNull List<String> courseCodes) {}
