package com.tecspace.portal.dto;

import java.time.Instant;

public record SubmissionRow(String studentId, String name, String status, String text, String link,
                            Instant submittedAt, boolean late, Double marks, String feedback) {}
