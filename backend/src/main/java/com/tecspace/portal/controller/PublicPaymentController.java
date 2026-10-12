package com.tecspace.portal.controller;

import com.tecspace.portal.dto.PaymentSessionResponse;
import com.tecspace.portal.service.QrPaymentService;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints opened by whoever scans the QR (no login). They are protected by the long random token in the URL.
 * In production the confirm step should come from your payment provider's webhook instead.
 */
@RestController
@RequestMapping("/api/pay")
public class PublicPaymentController {

    private final QrPaymentService qrPaymentService;

    public PublicPaymentController(QrPaymentService qrPaymentService) {
        this.qrPaymentService = qrPaymentService;
    }

    @GetMapping("/{token}")
    public PaymentSessionResponse info(@PathVariable String token) {
        return qrPaymentService.info(token);
    }

    @PostMapping("/{token}/scan")
    public PaymentSessionResponse scan(@PathVariable String token) {
        return qrPaymentService.markScanned(token);
    }

    @PostMapping("/{token}/confirm")
    public PaymentSessionResponse confirm(@PathVariable String token) {
        return qrPaymentService.confirm(token);
    }
}
