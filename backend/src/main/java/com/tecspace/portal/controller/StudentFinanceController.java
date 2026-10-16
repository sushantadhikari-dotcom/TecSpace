package com.tecspace.portal.controller;

import com.tecspace.portal.dto.LedgerView;
import com.tecspace.portal.dto.PlanView;
import com.tecspace.portal.dto.ReceiptView;
import com.tecspace.portal.dto.TranscriptView;
import com.tecspace.portal.model.Invoice;
import com.tecspace.portal.model.Scholarship;
import com.tecspace.portal.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Phase 3 student endpoints: invoices, scholarships, plan, ledger, receipts and transcript. */
@RestController
@RequestMapping("/api/student")
public class StudentFinanceController {

    private final UserService userService;
    private final FeeService feeService;
    private final ScholarshipService scholarshipService;
    private final PaymentPlanService planService;
    private final LedgerService ledgerService;
    private final TranscriptService transcriptService;

    public StudentFinanceController(UserService userService, FeeService feeService,
                                    ScholarshipService scholarshipService, PaymentPlanService planService,
                                    LedgerService ledgerService, TranscriptService transcriptService) {
        this.userService = userService;
        this.feeService = feeService;
        this.scholarshipService = scholarshipService;
        this.planService = planService;
        this.ledgerService = ledgerService;
        this.transcriptService = transcriptService;
    }

    @GetMapping("/invoices")
    public List<Invoice> invoices(Authentication auth) {
        return feeService.forStudent(userService.getByEmail(auth.getName()).getId());
    }

    @GetMapping("/scholarships")
    public List<Scholarship> scholarships(Authentication auth) {
        return scholarshipService.forStudent(userService.getByEmail(auth.getName()).getId());
    }

    @GetMapping("/payment-plan")
    public PlanView plan(Authentication auth) {
        return planService.view(userService.getByEmail(auth.getName()).getId());
    }

    @GetMapping("/ledger")
    public LedgerView ledger(Authentication auth) {
        return ledgerService.ledger(userService.getByEmail(auth.getName()).getId());
    }

    @GetMapping("/receipts/{transactionId}")
    public ReceiptView receipt(Authentication auth, @PathVariable String transactionId) {
        return ledgerService.receipt(userService.getByEmail(auth.getName()), transactionId);
    }

    @GetMapping("/transcript")
    public TranscriptView transcript(Authentication auth) {
        return transcriptService.build(userService.getByEmail(auth.getName()));
    }
}
