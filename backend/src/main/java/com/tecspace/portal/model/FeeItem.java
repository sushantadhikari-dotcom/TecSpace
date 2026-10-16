package com.tecspace.portal.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeItem {
    private String category;   // TUITION, LAB, HOSTEL, OTHER
    private String label;      // e.g. "Tuition", "Computer lab", "Hostel"
    private BigDecimal amount;
    private boolean optional;  // optional items (e.g. hostel) are only billed when chosen
}
