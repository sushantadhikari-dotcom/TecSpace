package com.tecspace.portal.dto;

import com.tecspace.portal.model.Trimester;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** GPA and CGPA are calculated by the server, so the admin only sends courses and grades. */
public record GradeUpdateRequest(@NotNull @Valid List<Trimester> trimesters) {}
