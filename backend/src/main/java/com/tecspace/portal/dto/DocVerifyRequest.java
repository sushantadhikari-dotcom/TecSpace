package com.tecspace.portal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DocVerifyRequest(@NotEmpty @Valid List<DocUpdate> documents) {}
