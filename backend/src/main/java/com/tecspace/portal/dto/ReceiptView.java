package com.tecspace.portal.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReceiptView(
        String receiptNumber, String transactionId, Instant date,
        String studentName, String email, String faculty, String session,
        BigDecimal amount, String method, BigDecimal balanceNow
) {}
