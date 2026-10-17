package com.tecspace.portal.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsView(
        int students,
        List<Bar> studentsByFaculty,
        List<Bar> studentsBySession,
        List<Bar> applicationsByStatus,
        List<Bar> courseFill,
        List<Bar> attendanceByCourse,
        int attendanceFlagged,
        List<Bar> gradeDistribution,
        List<Bar> avgCgpaByFaculty,
        List<Bar> monthlyCollections,
        BigDecimal billed,
        BigDecimal collected,
        BigDecimal outstanding
) {}
