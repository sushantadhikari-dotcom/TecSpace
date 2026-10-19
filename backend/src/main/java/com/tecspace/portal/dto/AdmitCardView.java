package com.tecspace.portal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdmitCardView(String studentId, String studentName, String email, String faculty, String session,
                            LocalDate issuedOn, boolean feeCleared, BigDecimal remainingBalance,
                            double attendanceMinimum, List<AdmitExam> exams, String verifyToken) {}
