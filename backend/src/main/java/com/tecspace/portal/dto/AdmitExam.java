package com.tecspace.portal.dto;

import java.time.LocalDate;
import java.util.List;

public record AdmitExam(String examId, String courseCode, String courseName, LocalDate date,
                        String startTime, String endTime, String room,
                        Double attendancePercent, boolean eligible, List<String> reasons) {}
