package com.tecspace.portal.service;

import com.tecspace.portal.dto.PaymentSessionResponse;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.FinancialRecord;
import com.tecspace.portal.model.PaymentSession;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.PaymentSessionRepository;
import com.tecspace.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Handles the QR payment lifecycle: create -> scanned -> completed. */
@Service
public class QrPaymentService {

    private static final List<String> ACTIVE = List.of(PaymentSession.PENDING, PaymentSession.SCANNED);

    private final PaymentSessionRepository sessions;
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final MongoTemplate mongo;

    @Value("${tecspace.payment.qr-expiry-minutes:10}")
    private long expiryMinutes;

    /** Demo switch. While true, the /pay page can confirm a payment. Set to false in production. */
    @Value("${tecspace.payment.simulate-confirm:true}")
    private boolean simulateConfirm;

    public QrPaymentService(PaymentSessionRepository sessions, PaymentService paymentService,
                            UserRepository userRepository, MongoTemplate mongo) {
        this.sessions = sessions;
        this.paymentService = paymentService;
        this.userRepository = userRepository;
        this.mongo = mongo;
    }

    // ---------------------------------------------------------------- student side

    /** Creates a QR for the student's FULL remaining balance. The student cannot pick the amount. */
    public PaymentSessionResponse create(User student) {
        FinancialRecord record = paymentService.getRecord(student.getId());
        BigDecimal due = record.getRemainingBalance();
        if (due.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Your fees are already fully paid");
        }

        // Only one live QR per student: cancel any older one.
        mongo.updateMulti(
                new Query(Criteria.where("studentId").is(student.getId()).and("status").in(ACTIVE)),
                new Update().set("status", PaymentSession.CANCELLED),
                PaymentSession.class);

        PaymentSession s = new PaymentSession();
        s.setToken(UUID.randomUUID().toString());
        s.setStudentId(student.getId());
        s.setAmount(due);
        s.setExpiresAt(Instant.now().plus(Duration.ofMinutes(expiryMinutes)));
        return PaymentSessionResponse.from(sessions.save(s), fullName(student));
    }

    /** The student's current live QR, if they have one (so a page refresh keeps the QR on screen). */
    public Optional<PaymentSessionResponse> active(User student) {
        return sessions.findByStudentIdAndStatusInOrderByCreatedAtDesc(student.getId(), ACTIVE).stream()
                .map(this::expireIfNeeded)
                .filter(s -> ACTIVE.contains(s.getStatus()))
                .findFirst()
                .map(s -> PaymentSessionResponse.from(s, fullName(student)));
    }

    public PaymentSessionResponse statusForStudent(User student, String token) {
        PaymentSession s = find(token);
        if (!s.getStudentId().equals(student.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Payment session not found");
        }
        return PaymentSessionResponse.from(expireIfNeeded(s), fullName(student));
    }

    // ---------------------------------------------------------------- payer side (public, by token)

    public PaymentSessionResponse info(String token) {
        PaymentSession s = expireIfNeeded(find(token));
        return toPublic(s);
    }

    /** Called when the QR is opened/scanned. Moves PENDING -> SCANNED. Safe to call repeatedly. */
    public PaymentSessionResponse markScanned(String token) {
        PaymentSession s = expireIfNeeded(find(token));
        if (PaymentSession.PENDING.equals(s.getStatus())) {
            PaymentSession updated = mongo.findAndModify(
                    new Query(Criteria.where("token").is(token).and("status").is(PaymentSession.PENDING)),
                    new Update().set("status", PaymentSession.SCANNED).set("scannedAt", Instant.now()),
                    FindAndModifyOptions.options().returnNew(true),
                    PaymentSession.class);
            if (updated != null) s = updated;
            else s = find(token);
        }
        return toPublic(s);
    }

    /** Completes the payment and records it. Only a SCANNED session can be completed, and only once. */
    public PaymentSessionResponse confirm(String token) {
        if (!simulateConfirm) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Payments are confirmed by the payment provider only");
        }
        expireIfNeeded(find(token));

        // Atomic claim: if two requests arrive together, only one gets the session back.
        PaymentSession claimed = mongo.findAndModify(
                new Query(Criteria.where("token").is(token).and("status").is(PaymentSession.SCANNED)),
                new Update().set("status", PaymentSession.COMPLETED).set("completedAt", Instant.now()),
                FindAndModifyOptions.options().returnNew(true),
                PaymentSession.class);

        if (claimed == null) {
            PaymentSession s = find(token);
            switch (s.getStatus()) {
                case PaymentSession.COMPLETED -> throw new ApiException(HttpStatus.CONFLICT, "This payment is already completed");
                case PaymentSession.PENDING -> throw new ApiException(HttpStatus.CONFLICT, "Scan the QR code first");
                case PaymentSession.EXPIRED -> throw new ApiException(HttpStatus.GONE, "This QR code has expired");
                default -> throw new ApiException(HttpStatus.GONE, "This QR code is no longer valid");
            }
        }

        try {
            paymentService.recordPayment(claimed.getStudentId(), claimed.getAmount(),
                    "QR_" + token.substring(0, 8).toUpperCase(), PaymentService.METHOD_QR);
        } catch (RuntimeException ex) {
            // e.g. the admin recorded cash in the meantime. Do not leave a COMPLETED session without a payment.
            mongo.updateFirst(new Query(Criteria.where("token").is(token)),
                    new Update().set("status", PaymentSession.CANCELLED).unset("completedAt"),
                    PaymentSession.class);
            throw ex;
        }
        return toPublic(claimed);
    }

    // ---------------------------------------------------------------- helpers

    private PaymentSession find(String token) {
        return sessions.findByToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payment session not found"));
    }

    private PaymentSession expireIfNeeded(PaymentSession s) {
        if (ACTIVE.contains(s.getStatus()) && s.getExpiresAt() != null && s.getExpiresAt().isBefore(Instant.now())) {
            s.setStatus(PaymentSession.EXPIRED);
            return sessions.save(s);
        }
        return s;
    }

    private PaymentSessionResponse toPublic(PaymentSession s) {
        String name = userRepository.findById(s.getStudentId()).map(this::fullName).orElse("a student");
        return PaymentSessionResponse.from(s, name);
    }

    private String fullName(User u) {
        return (u.getFirstName() + " " + u.getLastName()).trim();
    }
}
