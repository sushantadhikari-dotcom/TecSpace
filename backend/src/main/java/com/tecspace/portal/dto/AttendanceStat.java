package com.tecspace.portal.dto;

public record AttendanceStat(
        String studentId,
        String studentName,
        String courseCode,
        String courseName,
        int attended,
        int sessions,
        double percent,
        String level,          // NO_DATA, OK, WARNING, LOW
        int recentAbsences,    // absences in the last 5 counted sessions
        boolean trending       // 3 or more of the last 5 missed
) {}
