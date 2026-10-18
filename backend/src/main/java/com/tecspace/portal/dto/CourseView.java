package com.tecspace.portal.dto;

import java.util.List;

/**
 * status: NONE (not asked), PENDING (waiting for the administration), ENROLLED (approved), REJECTED (declined).
 * enrolled is true only when approved.
 */
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
        List<String> schedule,
        String status,
        String note,
        boolean hasSyllabus
) {}
