package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record RequestCreate(
        @NotBlank @Pattern(regexp = "TRANSCRIPT|ID_REISSUE|BONAFIDE|LEAVE", message = "is not a valid request type") String type,
        String details,
        LocalDate leaveFrom,
        LocalDate leaveTo
) {}
