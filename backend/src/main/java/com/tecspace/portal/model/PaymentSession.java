package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One QR payment attempt. The amount is fixed by the server when the QR is generated,
 * so the student can never choose or change it.
 *
 * status flow: PENDING -> SCANNED -> COMPLETED
 *              (or EXPIRED / CANCELLED at any point before COMPLETED)
 */
@Data
@Document(collection = "payment_sessions")
public class PaymentSession {
    public static final String PENDING = "PENDING";
    public static final String SCANNED = "SCANNED";
    public static final String COMPLETED = "COMPLETED";
    public static final String EXPIRED = "EXPIRED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    private String id;

    @Indexed
    private String pidx;
    private String paymentUrl;

    @Indexed(unique = true)
    private String token;

    @Indexed
    private String studentId;

    private BigDecimal amount;
    private String status = PENDING;

    private Instant createdAt = Instant.now();
    private Instant expiresAt;
    private Instant scannedAt;
    private Instant completedAt;
}
