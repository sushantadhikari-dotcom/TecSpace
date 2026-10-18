package com.tecspace.portal.dto;

import java.time.Instant;
import java.util.List;

/** One registration request as the administration sees it, with the checks already done. */
public record RegistrationRow(
        String id, String studentId, String studentName, String faculty,
        String courseCode, String courseName, String status, Instant requestedAt,
        String decidedBy, String note,
        int seatsLeft, int capacity, List<String> missingPrerequisites, String clash
) {}
