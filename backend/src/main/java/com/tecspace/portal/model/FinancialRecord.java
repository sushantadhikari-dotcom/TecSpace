package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "financials")
public class FinancialRecord {
    @Id
    private String id;

    @Version
    private Long version;

    @Indexed(unique = true)
    private String studentId;

    private BigDecimal totalFee = BigDecimal.ZERO;
    private BigDecimal paidFee = BigDecimal.ZERO;
    private BigDecimal remainingBalance = BigDecimal.ZERO;
    private List<PaymentTransaction> paymentHistory = new ArrayList<>();
}
