package com.tecspace.portal.controller;

import com.tecspace.portal.dto.AdminRequestView;
import com.tecspace.portal.dto.RequestDecision;
import com.tecspace.portal.service.RequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/requests")
public class AdminRequestController {

    private final RequestService requests;

    public AdminRequestController(RequestService requests) {
        this.requests = requests;
    }

    @GetMapping
    public List<AdminRequestView> list(@RequestParam(required = false) String status) {
        return requests.list(status);
    }

    @PostMapping("/{id}/decision")
    public ResponseEntity<Void> decide(Authentication auth, @PathVariable String id, @RequestBody RequestDecision req) {
        requests.decide(id, auth.getName(), req);
        return ResponseEntity.noContent().build();
    }
}
