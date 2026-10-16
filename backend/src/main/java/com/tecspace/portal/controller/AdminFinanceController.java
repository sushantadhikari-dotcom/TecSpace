package com.tecspace.portal.controller;

import com.tecspace.portal.dto.*;
import com.tecspace.portal.model.FeeStructure;
import com.tecspace.portal.model.Invoice;
import com.tecspace.portal.model.Scholarship;
import com.tecspace.portal.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Phase 3 admin endpoints: fee structures, invoices, scholarships, payment plans, ledger, collections. */
@RestController
@RequestMapping("/api/admin")
public class AdminFinanceController {

    private final FeeService feeService;
    private final ScholarshipService scholarshipService;
    private final PaymentPlanService planService;
    private final LedgerService ledgerService;

    public AdminFinanceController(FeeService feeService, ScholarshipService scholarshipService,
                                  PaymentPlanService planService, LedgerService ledgerService) {
        this.feeService = feeService;
        this.scholarshipService = scholarshipService;
        this.planService = planService;
        this.ledgerService = ledgerService;
    }

    // ---- fee structures
    @GetMapping("/fee-structures")
    public List<FeeStructure> structures() {
        return feeService.structures();
    }

    @PostMapping("/fee-structures")
    public ResponseEntity<FeeStructure> createStructure(@Valid @RequestBody FeeStructureRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feeService.createStructure(req));
    }

    @DeleteMapping("/fee-structures/{id}")
    public ResponseEntity<Void> deleteStructure(@PathVariable String id) {
        feeService.deleteStructure(id);
        return ResponseEntity.noContent().build();
    }

    // ---- invoices
    @GetMapping("/invoices")
    public List<Invoice> invoices() {
        return feeService.all();
    }

    @PostMapping("/invoices/generate")
    public Map<String, Integer> generate(@Valid @RequestBody GenerateInvoicesRequest req) {
        return feeService.generate(req);
    }

    @PostMapping("/invoices/{id}/cancel")
    public Invoice cancel(@PathVariable String id) {
        return feeService.cancel(id);
    }

    // ---- scholarships
    @GetMapping("/scholarships")
    public List<Scholarship> scholarships() {
        return scholarshipService.all();
    }

    @PostMapping("/scholarships")
    public ResponseEntity<Scholarship> award(@Valid @RequestBody ScholarshipRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scholarshipService.award(req));
    }

    @DeleteMapping("/scholarships/{id}")
    public Scholarship revoke(@PathVariable String id) {
        return scholarshipService.revoke(id);
    }

    // ---- payment plans
    @GetMapping("/payment-plans/{studentId}")
    public PlanView plan(@PathVariable String studentId) {
        return planService.view(studentId);
    }

    @PostMapping("/payment-plans")
    public PlanView createPlan(@Valid @RequestBody PlanRequest req) {
        return planService.create(req);
    }

    @DeleteMapping("/payment-plans/{studentId}")
    public ResponseEntity<Void> deletePlan(@PathVariable String studentId) {
        planService.delete(studentId);
        return ResponseEntity.noContent().build();
    }

    // ---- ledger and collections
    @GetMapping("/ledger/{studentId}")
    public LedgerView ledger(@PathVariable String studentId) {
        return ledgerService.ledger(studentId);
    }

    @GetMapping("/finance/summary")
    public FinanceSummary summary() {
        return ledgerService.summary();
    }
}
