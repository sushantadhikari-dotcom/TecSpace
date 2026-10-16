package com.tecspace.portal.dto;

import com.tecspace.portal.model.PaymentSession;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentSessionResponse(
        String token, BigDecimal amount, String status, Instant expiresAt,
        String studentName, String paymentUrl
) {
    public static PaymentSessionResponse from(PaymentSession s, String studentName) {
        return new PaymentSessionResponse(s.getToken(), s.getAmount(), s.getStatus(),
                s.getExpiresAt(), studentName, s.getPaymentUrl());
    }
}