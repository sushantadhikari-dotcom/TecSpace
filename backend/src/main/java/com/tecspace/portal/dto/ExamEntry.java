package com.tecspace.portal.dto;

import java.time.LocalDate;

public record ExamEntry(
        String id, String courseCode, String courseName,
        LocalDate date, String startTime, String endTime, String room, String note
) {}
