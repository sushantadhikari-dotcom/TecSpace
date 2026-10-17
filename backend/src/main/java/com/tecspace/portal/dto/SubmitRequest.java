package com.tecspace.portal.dto;

import jakarta.validation.constraints.Size;

public record SubmitRequest(@Size(max = 5000) String text, @Size(max = 500) String link) {}
