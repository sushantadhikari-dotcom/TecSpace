package com.tecspace.portal.controller;

import com.tecspace.portal.dto.*;
import com.tecspace.portal.model.Application;
import com.tecspace.portal.service.AdmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Applicants use /api/public (no login). Staff use /api/admin. */
@RestController
@RequestMapping("/api")
public class AdmissionController {

    private final AdmissionService admissions;

    public AdmissionController(AdmissionService admissions) {
        this.admissions = admissions;
    }

    // ---- applicant
    @PostMapping("/public/applications")
    public ResponseEntity<TrackView> apply(@Valid @RequestBody ApplyRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(admissions.apply(req));
    }

    @GetMapping("/public/applications/{code}")
    public TrackView track(@PathVariable String code, @RequestParam String email) {
        return admissions.track(code, email);
    }

    // ---- staff
    @GetMapping("/admin/applications")
    public List<Application> list(@RequestParam(required = false) String status) {
        return admissions.list(status);
    }

    @PutMapping("/admin/applications/{id}/documents")
    public Application documents(@PathVariable String id, @Valid @RequestBody DocVerifyRequest req) {
        return admissions.verifyDocuments(id, req.documents());
    }

    @PostMapping("/admin/applications/{id}/decision")
    public Application decide(Authentication auth, @PathVariable String id, @Valid @RequestBody DecisionRequest req) {
        return admissions.decide(id, auth.getName(), req);
    }

    @PostMapping("/admin/applications/{id}/enroll")
    public CreatedUser enroll(@PathVariable String id, @Valid @RequestBody EnrollApplicantRequest req) {
        return admissions.enroll(id, req.universityEmail());
    }

    @GetMapping("/admin/seat-config")
    public List<SeatStatus> seats() {
        return admissions.seatStatus();
    }

    @PutMapping("/admin/seat-config")
    public List<SeatStatus> setSeats(@Valid @RequestBody SeatConfigRequest req) {
        admissions.setSeats(req);
        return admissions.seatStatus();
    }
}
