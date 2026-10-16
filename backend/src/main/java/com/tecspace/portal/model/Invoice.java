package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "invoices")
public class Invoice {
    public static final String ISSUED = "ISSUED";
    public static final String CANCELLED = "CANCELLED";

    @Id
    private String id;

    @Indexed(unique = true)
    private String invoiceNumber;

    @Indexed
    private String studentId;

    private String structureId;
    private String structureName;
    private String term;
    private List<InvoiceItem> items = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private String status = ISSUED;
    private LocalDate dueDate;
    private Instant issuedAt = Instant.now();
}
