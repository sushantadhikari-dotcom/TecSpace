package com.tecspace.portal.dto;

import com.tecspace.portal.model.Announcement;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        String term,
        BigDecimal remainingBalance,
        int enrolledCount,
        int enrolledCredits,
        List<ClassEntry> todayClasses,
        List<Announcement> announcements
) {}
