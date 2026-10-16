package com.tecspace.portal.dto;

import java.util.List;

public record CourseView(
        String code,
        String name,
        String department,
        int creditHours,
        int capacity,
        int enrolledCount,
        int seatsLeft,
        String instructorName,
        List<String> prerequisites,
        boolean enrolled,
        List<String> schedule
) {}
