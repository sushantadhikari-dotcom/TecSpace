package com.tecspace.portal.dto;

import java.math.BigDecimal;

public record FinanceRow(String studentId, String name, String faculty, BigDecimal totalFee, BigDecimal paidFee, BigDecimal remaining) {}
