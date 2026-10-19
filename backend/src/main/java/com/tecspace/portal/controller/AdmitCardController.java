package com.tecspace.portal.controller;

import com.tecspace.portal.dto.AdmitCardView;
import com.tecspace.portal.dto.VerifyView;
import com.tecspace.portal.service.AdmitCardService;
import com.tecspace.portal.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AdmitCardController {

    private final AdmitCardService admitCards;
    private final UserService userService;

    public AdmitCardController(AdmitCardService admitCards, UserService userService) {
        this.admitCards = admitCards;
        this.userService = userService;
    }

    @GetMapping("/student/admit-card")
    public AdmitCardView mine(Authentication auth) {
        return admitCards.forStudent(userService.getByEmail(auth.getName()));
    }

    /** Opened by whoever scans the QR code on the card. */
    @GetMapping("/public/admit-card/verify")
    public VerifyView verify(@RequestParam String student, @RequestParam String token) {
        return admitCards.verify(student, token);
    }
}
