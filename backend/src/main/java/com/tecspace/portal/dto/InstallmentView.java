package com.tecspace.portal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentView(LocalDate dueDate, BigDecimal amount, BigDecimal paid, String status) {}
