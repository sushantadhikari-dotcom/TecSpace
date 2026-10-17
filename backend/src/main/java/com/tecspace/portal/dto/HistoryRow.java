package com.tecspace.portal.dto;

import java.time.Instant;

public record HistoryRow(String courseCode, String courseName, String term, String status, Instant enrolledAt) {}
