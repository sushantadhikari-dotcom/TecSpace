package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Splits what the student still owes into installments. One plan per student. */
@Data
@Document(collection = "payment_plans")
public class PaymentPlan {
    @Id
    private String id;

    @Indexed(unique = true)
    private String studentId;

    private BigDecimal paidAtCreation = BigDecimal.ZERO;
    private List<Installment> installments = new ArrayList<>();
    private Instant createdAt = Instant.now();
}
