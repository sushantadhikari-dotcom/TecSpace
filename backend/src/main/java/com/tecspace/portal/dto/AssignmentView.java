package com.tecspace.portal.dto;

import java.time.LocalDateTime;

public record AssignmentView(String id, String courseCode, String courseName, String title, String description,
                             LocalDateTime dueAt, double maxMarks, int submissionCount, int gradedCount, int studentCount) {}
