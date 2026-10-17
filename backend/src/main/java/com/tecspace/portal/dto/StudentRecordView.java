package com.tecspace.portal.dto;

import java.util.List;

public record StudentRecordView(StudentSummary student, ProfileData profile, List<HistoryRow> history) {}
