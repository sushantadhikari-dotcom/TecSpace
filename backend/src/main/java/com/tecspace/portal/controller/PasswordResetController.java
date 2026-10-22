package com.tecspace.portal.controller;

import com.tecspace.portal.dto.ForgotPasswordRequest;
import com.tecspace.portal.dto.ResetPasswordRequest;
import com.tecspace.portal.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PasswordResetController {

    private final PasswordResetService resets;

    public PasswordResetController(PasswordResetService resets) {
        this.resets = resets;
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        try {
            resets.request(req.email());
        } catch (PasswordResetService.TooSoon ignored) {
            // same answer as always
        }
        return Map.of("message", "If that email has an account, we sent a reset link. Check your inbox.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest req) {
        resets.reset(req.token(), req.newPassword());
        return ResponseEntity.noContent().build();
    }
}
