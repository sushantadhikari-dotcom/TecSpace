package com.tecspace.portal.dto;

import java.time.Instant;
import java.time.LocalDate;

public record AdminRequestView(String id, String studentId, String studentName, String faculty, String type,
                               String details, LocalDate leaveFrom, LocalDate leaveTo, String status,
                               String adminNote, String decidedBy, Instant createdAt, Instant decidedAt) {}
