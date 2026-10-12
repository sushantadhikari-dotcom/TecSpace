package com.tecspace.portal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    private String transactionId;
    private BigDecimal amount;
    private String method;     // QR_PAYMENT, CASH_PAYMENT, ADMIN_ADJUSTMENT
    private Instant timestamp;
}
