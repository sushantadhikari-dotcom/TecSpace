package com.tecspace.portal.dto;

public record GradebookRow(
        String studentId, String name,
        double courseworkEarned, double courseworkPossible, Double courseworkPercent, int gradedCount,
        Double finalPercentage, String grade
) {}
