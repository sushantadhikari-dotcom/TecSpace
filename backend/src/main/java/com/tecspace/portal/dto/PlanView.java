package com.tecspace.portal.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlanView(boolean exists, BigDecimal totalAmount, BigDecimal totalPaid, List<InstallmentView> installments) {}
