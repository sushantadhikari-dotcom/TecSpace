package com.tecspace.portal.dto;

import java.util.List;

public record VerifyView(boolean valid, String studentName, String faculty, String session, List<AdmitExam> exams) {}
