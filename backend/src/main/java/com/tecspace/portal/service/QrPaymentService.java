package com.tecspace.portal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecspace.portal.dto.EsewaFormResponse;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * QR payment lifecycle with eSewa: create -> payer opens QR link -> eSewa checkout -> verify (eSewa status check).
 * A payment is only recorded after eSewa itself says COMPLETE for the exact amount.
 */
@Service
public class QrPaymentService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QrPaymentService.class);
    private static final List<String> ACTIVE = List.of(PaymentSession.PENDING, PaymentSession.SCANNED);

    private final PaymentSessionRepository sessions;
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final MongoTemplate mongo;
    private final EsewaClient esewa;
    private final ObjectMapper json = new ObjectMapper();

    @Value("${tecspace.payment.qr-expiry-minutes:10}")
    private long expiryMinutes;

    public QrPaymentService(PaymentSessionRepository sessions, PaymentService paymentService,
                            UserRepository userRepository, MongoTemplate mongo, EsewaClient esewa) {
        this.sessions = sessions;
        this.paymentService = paymentService;
        this.userRepository = userRepository;
        this.mongo = mongo;
        this.esewa = esewa;
    }

    // ---------------------------------------------------------------- student side

    /** Creates a payment session for the student's FULL remaining balance. The student cannot pick the amount. */
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
        s.setToken(UUID.randomUUID().toString());   // also used as eSewa's transaction_uuid
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

    // ---------------------------------------------------------------- payer side (public)

    public PaymentSessionResponse info(String token) {
        return toPublic(expireIfNeeded(find(token)));
    }

    /** The form the payer's browser submits to eSewa. Only available while the QR is live. */
    public EsewaFormResponse esewaForm(String token) {
        PaymentSession s = expireIfNeeded(find(token));
        if (!ACTIVE.contains(s.getStatus())) {
            throw new ApiException(HttpStatus.GONE, "This QR code is no longer valid. Ask the student for a new one.");
        }
        return new EsewaFormResponse(esewa.formUrl(), esewa.formFields(s.getToken(), s.getAmount()));
    }

    /**
     * Called from the page eSewa sends the payer back to. `data` is eSewa's Base64 callback.
     * We only use it to find the session, then ask eSewa directly. Safe to call more than once.
     */
    public PaymentSessionResponse verify(String data) {
        String token = transactionUuidFrom(data);
        PaymentSession s = find(token);
        if (PaymentSession.COMPLETED.equals(s.getStatus())) {
            return toPublic(s);
        }

        Map<String, Object> res;
        try {
            res = esewa.status(token, s.getAmount());
        } catch (RestClientException ex) {
            throw esewaError(ex);
        }

        String status = String.valueOf(res.get("status"));
        boolean amountMatches;
        try {
            amountMatches = new BigDecimal(String.valueOf(res.get("total_amount"))).compareTo(s.getAmount()) == 0;
        } catch (NumberFormatException ex) {
            amountMatches = false;
        }
        if (!"COMPLETE".equals(status) || !amountMatches) {
            throw new ApiException(HttpStatus.CONFLICT, "Payment not completed (eSewa status: " + status + ")");
        }

        // Atomic claim: if two requests arrive together, only one gets the session back.
        // EXPIRED is allowed because the payer may have paid just after our QR timer ran out.
        PaymentSession claimed = mongo.findAndModify(
                new Query(Criteria.where("token").is(token)
                        .and("status").in(PaymentSession.PENDING, PaymentSession.SCANNED, PaymentSession.EXPIRED)),
                new Update().set("status", PaymentSession.COMPLETED).set("completedAt", Instant.now()),
                FindAndModifyOptions.options().returnNew(true),
                PaymentSession.class);

        if (claimed == null) {
            PaymentSession latest = find(token);
            if (PaymentSession.COMPLETED.equals(latest.getStatus())) return toPublic(latest);
            throw new ApiException(HttpStatus.GONE, "This payment session is no longer valid");
        }

        Object ref = res.get("ref_id");
        String txnRef = "ESEWA_" + (ref != null ? ref : token.substring(0, 8).toUpperCase());
        try {
            paymentService.recordPayment(claimed.getStudentId(), claimed.getAmount(), txnRef, PaymentService.METHOD_QR);
        } catch (RuntimeException ex) {
            // Money was taken but could not be recorded (e.g. the admin entered cash in the meantime).
            // Flag it for manual review / refund instead of silently losing it.
            mongo.updateFirst(new Query(Criteria.where("token").is(token)),
                    new Update().set("status", "REVIEW"),
                    PaymentSession.class);
            throw ex;
        }
        return toPublic(claimed);
    }

    // ---------------------------------------------------------------- helpers

    private String transactionUuidFrom(String data) {
        if (data == null || data.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing payment data");
        }
        try {
            // A browser turns "+" into a space inside a query string, so put it back.
            byte[] bytes = Base64.getDecoder().decode(data.trim().replace(' ', '+'));
            Object uuid = json.readValue(new String(bytes, StandardCharsets.UTF_8), Map.class).get("transaction_uuid");
            if (uuid == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid payment data");
            return String.valueOf(uuid);
        } catch (IllegalArgumentException | JsonProcessingException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid payment data");
        }
    }

    private ApiException esewaError(RestClientException ex) {
        if (ex instanceof RestClientResponseException r) {
            log.warn("eSewa refused the request: {} {}", r.getStatusCode().value(), r.getResponseBodyAsString());
            return new ApiException(HttpStatus.BAD_GATEWAY,
                    "eSewa said (" + r.getStatusCode().value() + "): " + r.getResponseBodyAsString());
        }
        log.warn("Could not reach eSewa", ex);
        return new ApiException(HttpStatus.BAD_GATEWAY, "Could not reach eSewa: " + ex.getMessage());
    }

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