package com.tecspace.portal.service;

import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.FinancialRecord;
import com.tecspace.portal.repository.FinancialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** The one place that changes a student's total fee when invoices or scholarships change. */
@Service
public class BalanceService {

    private final FinancialRepository financials;

    public BalanceService(FinancialRepository financials) {
        this.financials = financials;
    }

    public FinancialRecord record(String studentId) {
        return financials.findByStudentId(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Financial record not found"));
    }

    /** delta > 0 adds to what the student owes, delta < 0 reduces it. */
    public FinancialRecord adjustTotal(String studentId, BigDecimal delta) {
        FinancialRecord r = record(studentId);
        BigDecimal total = r.getTotalFee().add(delta).max(BigDecimal.ZERO);
        r.setTotalFee(total);
        r.setRemainingBalance(total.subtract(r.getPaidFee()).max(BigDecimal.ZERO));
        return financials.save(r);
    }
}
