package com.tecspace.portal.controller;

import com.tecspace.portal.dto.FinancialResponse;
import com.tecspace.portal.dto.PaymentSessionResponse;
import com.tecspace.portal.model.GradeRecord;
import com.tecspace.portal.model.LibraryCard;
import com.tecspace.portal.model.User;
import com.tecspace.portal.service.GradeService;
import com.tecspace.portal.service.LibraryCardService;
import com.tecspace.portal.service.PaymentService;
import com.tecspace.portal.service.QrPaymentService;
import com.tecspace.portal.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final UserService userService;
    private final PaymentService paymentService;
    private final GradeService gradeService;
    private final LibraryCardService libraryCardService;
    private final QrPaymentService qrPaymentService;

    public StudentController(UserService userService, PaymentService paymentService,
                             GradeService gradeService, LibraryCardService libraryCardService,
                             QrPaymentService qrPaymentService) {
        this.userService = userService;
        this.paymentService = paymentService;
        this.gradeService = gradeService;
        this.libraryCardService = libraryCardService;
        this.qrPaymentService = qrPaymentService;
    }

    @GetMapping("/financials")
    public FinancialResponse financials(Authentication auth) {
        User me = userService.getByEmail(auth.getName());
        return paymentService.getFinancials(me.getId());
    }

    /** Creates a QR for the full remaining balance. The student cannot choose or edit the amount. */
    @PostMapping("/payment-sessions")
    public ResponseEntity<PaymentSessionResponse> createPaymentSession(Authentication auth) {
        User me = userService.getByEmail(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(qrPaymentService.create(me));
    }

    @GetMapping("/payment-sessions/active")
    public ResponseEntity<PaymentSessionResponse> activePaymentSession(Authentication auth) {
        User me = userService.getByEmail(auth.getName());
        return qrPaymentService.active(me)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/payment-sessions/{token}")
    public PaymentSessionResponse paymentSession(Authentication auth, @PathVariable String token) {
        User me = userService.getByEmail(auth.getName());
        return qrPaymentService.statusForStudent(me, token);
    }

    @GetMapping("/grades")
    public GradeRecord grades(Authentication auth) {
        User me = userService.getByEmail(auth.getName());
        return gradeService.get(me.getId());
    }

    @PostMapping("/library-card/apply")
    public ResponseEntity<LibraryCard> applyLibraryCard(Authentication auth) {
        User me = userService.getByEmail(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryCardService.apply(me));
    }

    @GetMapping("/library-card")
    public LibraryCard libraryCard(Authentication auth) {
        User me = userService.getByEmail(auth.getName());
        return libraryCardService.get(me.getId());
    }
}
