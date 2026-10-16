package com.tecspace.portal.dto;

public record ClassEntry(
        String id,
        String courseCode,
        String courseName,
        String instructorName,
        String day,
        String startTime,
        String endTime,
        String room
) {}
