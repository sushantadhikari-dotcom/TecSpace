package com.tecspace.portal.service;

import com.tecspace.portal.dto.ScholarshipRequest;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.FinancialRecord;
import com.tecspace.portal.model.Invoice;
import com.tecspace.portal.model.Scholarship;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.InvoiceRepository;
import com.tecspace.portal.repository.ScholarshipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ScholarshipService {

    private final ScholarshipRepository scholarships;
    private final InvoiceRepository invoices;
    private final UserService userService;
    private final BalanceService balance;
    private final CourseOfferingService courseService;
    private final NotificationService notifications;

    public ScholarshipService(ScholarshipRepository scholarships, InvoiceRepository invoices, UserService userService,
                              BalanceService balance, CourseOfferingService courseService,
                              NotificationService notifications) {
        this.scholarships = scholarships;
        this.invoices = invoices;
        this.userService = userService;
        this.balance = balance;
        this.courseService = courseService;
        this.notifications = notifications;
    }

    /** PERCENT is worked out on the student's issued invoices for the current term. */
    public Scholarship award(ScholarshipRequest r) {
        User student = userService.getStudentById(r.studentId());
        String term = courseService.term();

        BigDecimal billed = invoices.findByStudentIdOrderByIssuedAtDesc(student.getId()).stream()
                .filter(i -> Invoice.ISSUED.equals(i.getStatus()) && term.equals(i.getTerm()))
                .map(Invoice::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (billed.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Issue this student's invoice for " + term + " first");
        }

        BigDecimal amount;
        if ("PERCENT".equals(r.type())) {
            if (r.value().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A percentage cannot be more than 100");
            }
            amount = billed.multiply(r.value()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            amount = r.value().setScale(2, RoundingMode.HALF_UP);
        }

        FinancialRecord rec = balance.record(student.getId());
        if (amount.compareTo(billed) > 0
                || rec.getTotalFee().subtract(amount).compareTo(rec.getPaidFee()) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This discount is too large: it would take the fees below what the student has billed or already paid");
        }

        Scholarship s = new Scholarship();
        s.setStudentId(student.getId());
        s.setName(r.name().trim());
        s.setType(r.type());
        s.setValue(r.value());
        s.setAmount(amount);
        s.setTerm(term);
        s = scholarships.save(s);

        balance.adjustTotal(student.getId(), amount.negate());
        notifications.notify(student.getId(), "SYSTEM", "Scholarship awarded: " + s.getName(),
                amount.toPlainString() + " was taken off your fees for " + term + ".", null);
        return s;
    }

    public Scholarship revoke(String id) {
        Scholarship s = scholarships.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Scholarship not found"));
        if (!Scholarship.ACTIVE.equals(s.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This scholarship is already revoked");
        }
        s.setStatus(Scholarship.REVOKED);
        scholarships.save(s);
        balance.adjustTotal(s.getStudentId(), s.getAmount());
        return s;
    }

    public List<Scholarship> all() {
        return scholarships.findAllByOrderByAwardedAtDesc();
    }

    public List<Scholarship> forStudent(String studentId) {
        return scholarships.findByStudentIdOrderByAwardedAtDesc(studentId);
    }
}
