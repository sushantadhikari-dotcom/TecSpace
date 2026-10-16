package com.tecspace.portal.controller;

import com.tecspace.portal.dto.EsewaFormResponse;
import com.tecspace.portal.dto.PaymentSessionResponse;
import com.tecspace.portal.service.QrPaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public endpoints (no login) used around the eSewa checkout.
 * Nothing here can mark a payment as paid by itself: /verify only succeeds
 * when eSewa's own status check says the payment is COMPLETE.
 */
@RestController
@RequestMapping("/api/pay")
public class PublicPaymentController {

    private final QrPaymentService qrPaymentService;

    public PublicPaymentController(QrPaymentService qrPaymentService) {
        this.qrPaymentService = qrPaymentService;
    }

    /** Status of a payment session, found by its random token. */
    @GetMapping("/{token}")
    public PaymentSessionResponse info(@PathVariable String token) {
        return qrPaymentService.info(token);
    }

    /** The signed form fields the payer's browser submits to eSewa. */
    @GetMapping("/{token}/esewa-form")
    public EsewaFormResponse esewaForm(@PathVariable String token) {
        return qrPaymentService.esewaForm(token);
    }

    /** Called by /pay/return after eSewa redirects the payer back. Body: { "data": "<base64 from eSewa>" } */
    @PostMapping("/verify")
    public PaymentSessionResponse verify(@RequestBody Map<String, String> body) {
        return qrPaymentService.verify(body.get("data"));
    }
}