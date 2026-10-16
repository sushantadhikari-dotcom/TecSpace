package com.tecspace.portal.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinanceSummary(
        BigDecimal totalBilled, BigDecimal totalCollected, BigDecimal totalOutstanding,
        int studentCount, int fullyPaidCount, List<FinanceRow> rows
) {}
