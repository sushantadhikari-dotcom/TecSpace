package com.tecspace.portal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CashPaymentRequest(
        @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal amount
) {}
