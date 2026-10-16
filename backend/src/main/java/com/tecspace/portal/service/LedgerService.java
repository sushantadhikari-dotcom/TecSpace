package com.tecspace.portal.service;

import com.tecspace.portal.dto.*;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.*;
import com.tecspace.portal.repository.FinancialRepository;
import com.tecspace.portal.repository.InvoiceRepository;
import com.tecspace.portal.repository.ScholarshipRepository;
import com.tecspace.portal.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    private record Raw(Instant when, String type, String description, BigDecimal debit, BigDecimal credit) {}

    private final InvoiceRepository invoices;
    private final ScholarshipRepository scholarships;
    private final FinancialRepository financials;
    private final UserRepository users;
    private final BalanceService balance;
    private final UserService userService;

    public LedgerService(InvoiceRepository invoices, ScholarshipRepository scholarships,
                         FinancialRepository financials, UserRepository users,
                         BalanceService balance, UserService userService) {
        this.invoices = invoices;
        this.scholarships = scholarships;
        this.financials = financials;
        this.users = users;
        this.balance = balance;
        this.userService = userService;
    }

    /**
     * Every invoice (debit), scholarship (credit) and payment (credit) in date order with a running balance.
     * If fees were also set by hand, one extra "opening balance / adjustments" line makes the ledger match
     * the student's real remaining balance.
     */
    public LedgerView ledger(String studentId) {
        User student = userService.getStudentById(studentId);
        FinancialRecord rec = balance.record(studentId);

        List<Raw> raw = new ArrayList<>();
        for (Invoice i : invoices.findByStudentIdOrderByIssuedAtDesc(studentId)) {
            if (!Invoice.ISSUED.equals(i.getStatus())) continue;
            raw.add(new Raw(i.getIssuedAt(), "INVOICE", "Invoice " + i.getInvoiceNumber() + " · " + i.getStructureName(),
                    i.getSubtotal(), BigDecimal.ZERO));
        }
        for (Scholarship s : scholarships.findByStudentIdOrderByAwardedAtDesc(studentId)) {
            if (!Scholarship.ACTIVE.equals(s.getStatus())) continue;
            raw.add(new Raw(s.getAwardedAt(), "SCHOLARSHIP", "Scholarship · " + s.getName(), BigDecimal.ZERO, s.getAmount()));
        }
        for (PaymentTransaction t : rec.getPaymentHistory()) {
            boolean refund = t.getAmount().signum() < 0;
            String label = "QR_PAYMENT".equals(t.getMethod()) ? "Online payment" : "Payment recorded by admin";
            raw.add(new Raw(t.getTimestamp(), "PAYMENT", label + " (" + t.getTransactionId() + ")",
                    refund ? t.getAmount().negate() : BigDecimal.ZERO, refund ? BigDecimal.ZERO : t.getAmount()));
        }

        BigDecimal debits = raw.stream().map(Raw::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = raw.stream().map(Raw::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diff = rec.getRemainingBalance().subtract(debits.subtract(credits));
        if (diff.signum() != 0) {
            Instant earliest = raw.stream().map(Raw::when).min(Comparator.naturalOrder()).orElse(Instant.now());
            raw.add(new Raw(earliest.minusSeconds(1), "ADJUSTMENT", "Opening balance / manual adjustments",
                    diff.signum() > 0 ? diff : BigDecimal.ZERO, diff.signum() < 0 ? diff.negate() : BigDecimal.ZERO));
        }

        raw.sort(Comparator.comparing(Raw::when));
        List<LedgerEntry> entries = new ArrayList<>();
        BigDecimal running = BigDecimal.ZERO;
        for (Raw r : raw) {
            running = running.add(r.debit()).subtract(r.credit());
            entries.add(new LedgerEntry(r.when(), r.type(), r.description(), r.debit(), r.credit(), running));
        }
        return new LedgerView(fullName(student), rec.getTotalFee(), rec.getPaidFee(), rec.getRemainingBalance(), entries);
    }

    public ReceiptView receipt(User student, String transactionId) {
        FinancialRecord rec = balance.record(student.getId());
        PaymentTransaction t = rec.getPaymentHistory().stream()
                .filter(x -> x.getTransactionId().equals(transactionId)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Receipt not found"));
        return new ReceiptView("RCT-" + t.getTransactionId(), t.getTransactionId(), t.getTimestamp(),
                fullName(student), student.getUniversityEmail(), student.getFaculty(), student.getSession(),
                t.getAmount(), t.getMethod(), rec.getRemainingBalance());
    }

    /** Collections overview for the whole institution. */
    public FinanceSummary summary() {
        Map<String, FinancialRecord> byStudent = financials.findAll().stream()
                .collect(Collectors.toMap(FinancialRecord::getStudentId, r -> r, (a, b) -> a));
        List<FinanceRow> rows = new ArrayList<>();
        BigDecimal billed = BigDecimal.ZERO;
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        int fullyPaid = 0;

        for (User u : users.findByRole("ROLE_STUDENT")) {
            FinancialRecord r = byStudent.get(u.getId());
            if (r == null) continue;
            rows.add(new FinanceRow(u.getId(), fullName(u), u.getFaculty(), r.getTotalFee(), r.getPaidFee(), r.getRemainingBalance()));
            billed = billed.add(r.getTotalFee());
            collected = collected.add(r.getPaidFee());
            outstanding = outstanding.add(r.getRemainingBalance());
            if (r.getRemainingBalance().signum() <= 0 && r.getTotalFee().signum() > 0) fullyPaid++;
        }
        rows.sort(Comparator.comparing(FinanceRow::remaining).reversed());
        return new FinanceSummary(billed, collected, outstanding, rows.size(), fullyPaid, rows);
    }

    private String fullName(User u) {
        return (u.getFirstName() + " " + u.getLastName()).trim();
    }
}
