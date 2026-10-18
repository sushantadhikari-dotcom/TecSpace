package com.tecspace.portal.controller;

import com.tecspace.portal.dto.DirectEnrollRequest;
import com.tecspace.portal.dto.RegistrationDecision;
import com.tecspace.portal.dto.RegistrationRow;
import com.tecspace.portal.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Course registration is decided by the administration. */
@RestController
@RequestMapping("/api/admin/enrollment-requests")
public class AdminRegistrationController {

    private final RegistrationService registrations;

    public AdminRegistrationController(RegistrationService registrations) {
        this.registrations = registrations;
    }

    @GetMapping
    public List<RegistrationRow> list(@RequestParam(required = false) String status) {
        return registrations.list(status);
    }

    @PostMapping("/{id}/decision")
    public ResponseEntity<Void> decide(Authentication auth, @PathVariable String id, @RequestBody RegistrationDecision req) {
        registrations.decide(id, auth.getName(), req);
        return ResponseEntity.noContent().build();
    }

    /** Register a student straight into a course (same checks as an approval). */
    @PostMapping("/direct")
    public ResponseEntity<Void> direct(Authentication auth, @Valid @RequestBody DirectEnrollRequest req) {
        registrations.directEnroll(auth.getName(), req);
        return ResponseEntity.noContent().build();
    }
}
