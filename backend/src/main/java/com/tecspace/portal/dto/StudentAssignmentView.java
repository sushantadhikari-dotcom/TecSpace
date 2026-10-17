package com.tecspace.portal.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record StudentAssignmentView(
        String id, String courseCode, String courseName, String title, String description,
        LocalDateTime dueAt, double maxMarks,
        String status,            // NOT_SUBMITTED, SUBMITTED, LATE, GRADED
        boolean overdue,          // past the deadline and nothing handed in
        String text, String link, Instant submittedAt, Double marks, String feedback
) {}
