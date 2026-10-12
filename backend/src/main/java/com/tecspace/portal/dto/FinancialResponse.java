package com.tecspace.portal.dto;

import com.tecspace.portal.model.PaymentTransaction;

import java.math.BigDecimal;
import java.util.List;

public record FinancialResponse(
        BigDecimal totalFee,
        BigDecimal paidFee,
        BigDecimal remainingBalance,
        List<PaymentTransaction> paymentHistory
) {}
