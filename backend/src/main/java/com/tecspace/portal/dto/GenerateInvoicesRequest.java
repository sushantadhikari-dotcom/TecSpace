package com.tecspace.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** studentId empty = every student of the structure's faculty. includeOptional = labels of optional items to bill. */
public record GenerateInvoicesRequest(
        @NotBlank String structureId,
        String studentId,
        List<String> includeOptional,
        @NotNull LocalDate dueDate
) {}
