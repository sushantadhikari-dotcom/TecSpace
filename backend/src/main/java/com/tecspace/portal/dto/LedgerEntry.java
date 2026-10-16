package com.tecspace.portal.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntry(Instant date, String type, String description, BigDecimal debit, BigDecimal credit, BigDecimal balance) {}
