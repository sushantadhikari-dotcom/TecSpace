package com.tecspace.portal.dto;

import java.math.BigDecimal;
import java.util.List;

public record LedgerView(String studentName, BigDecimal totalFee, BigDecimal paidFee, BigDecimal remainingBalance, List<LedgerEntry> entries) {}
