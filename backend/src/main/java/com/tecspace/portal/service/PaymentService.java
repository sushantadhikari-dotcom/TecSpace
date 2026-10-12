package com.tecspace.portal.service;

import com.tecspace.portal.dto.AdminFinancialRequest;
import com.tecspace.portal.dto.FinancialResponse;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.FinancialRecord;
import com.tecspace.portal.model.PaymentTransaction;
import com.tecspace.portal.repository.FinancialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PaymentService {

    public static final String METHOD_QR = "QR_PAYMENT";
    public static final String METHOD_CASH = "CASH_PAYMENT";

    private final FinancialRepository financialRepository;

    public PaymentService(FinancialRepository financialRepository) {
        this.financialRepository = financialRepository;
    }

    public FinancialRecord getRecord(String studentId) {
        return financialRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Financial record not found"));
    }

    public FinancialResponse getFinancials(String studentId) {
        return toResponse(getRecord(studentId));
    }

    /**
     * Adds a payment to the student's record. Used by the QR flow (after the payment is confirmed)
     * and by the admin cash entry. Students cannot call this directly.
     */
    public FinancialResponse recordPayment(String studentId, BigDecimal amountPaid, String txnRef, String method) {
        FinancialRecord record = getRecord(studentId);

        boolean duplicate = record.getPaymentHistory().stream()
                .anyMatch(t -> t.getTransactionId().equals(txnRef));
        if (duplicate) {
            throw new ApiException(HttpStatus.CONFLICT, "This transaction reference was already used");
        }
        if (record.getRemainingBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Fees are already fully paid");
        }
        if (amountPaid.compareTo(record.getRemainingBalance()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Amount is more than the remaining balance");
        }

        BigDecimal newPaid = record.getPaidFee().add(amountPaid);
        record.setPaidFee(newPaid);
        record.setRemainingBalance(record.getTotalFee().subtract(newPaid).max(BigDecimal.ZERO));
        record.getPaymentHistory().add(new PaymentTransaction(txnRef, amountPaid, method, Instant.now()));

        return toResponse(financialRepository.save(record));
    }

    /** Admin records cash handed over by a student. */
    public FinancialResponse recordCashPayment(String studentId, BigDecimal amount) {
        return recordPayment(studentId, amount, "CASH_" + System.currentTimeMillis(), METHOD_CASH);
    }

    public FinancialResponse adminUpdate(String studentId, AdminFinancialRequest req) {
        FinancialRecord record = getRecord(studentId);

        if (req.paidFee().compareTo(req.totalFee()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Paid fee cannot be more than the total fee");
        }

        BigDecimal difference = req.paidFee().subtract(record.getPaidFee());
        if (difference.signum() != 0) {
            record.getPaymentHistory().add(new PaymentTransaction(
                    "ADM_" + System.currentTimeMillis(), difference, "ADMIN_ADJUSTMENT", Instant.now()));
        }
        record.setTotalFee(req.totalFee());
        record.setPaidFee(req.paidFee());
        record.setRemainingBalance(req.totalFee().subtract(req.paidFee()).max(BigDecimal.ZERO));

        return toResponse(financialRepository.save(record));
    }

    private FinancialResponse toResponse(FinancialRecord r) {
        return new FinancialResponse(r.getTotalFee(), r.getPaidFee(), r.getRemainingBalance(), r.getPaymentHistory());
    }
}
