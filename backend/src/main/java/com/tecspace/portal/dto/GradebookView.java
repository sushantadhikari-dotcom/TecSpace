package com.tecspace.portal.dto;

import java.util.List;

public record GradebookView(String courseCode, String courseName, String trimesterName, boolean locked,
                            int assignmentCount, List<GradebookRow> rows) {}
