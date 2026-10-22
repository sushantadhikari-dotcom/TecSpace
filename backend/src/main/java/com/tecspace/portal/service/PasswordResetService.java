package com.tecspace.portal.service;

import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.PasswordResetToken;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.PasswordResetTokenRepository;
import com.tecspace.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/** "Forgot password": a one-time link that works for 30 minutes. */
@Service
public class PasswordResetService {

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final EmailService email;
    private final SecureRandom random = new SecureRandom();

    @Value("${tecspace.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens,
                                PasswordEncoder encoder, EmailService email) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.email = email;
    }

    /** Always looks the same from outside, so nobody can find out which emails have accounts. */
    public void request(String emailAddress) {
        User user = users.findByUniversityEmail(emailAddress.trim().toLowerCase()).orElse(null);
        if (user == null || !user.isActive()) return;

        // at most one link every 2 minutes per account
        tokens.findTopByUserIdOrderByCreatedAtDesc(user.getId()).ifPresent(last -> {
            if (last.getCreatedAt().isAfter(Instant.now().minus(Duration.ofMinutes(2)))) {
                throw new TooSoon();
            }
        });

        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        PasswordResetToken t = new PasswordResetToken();
        t.setUserId(user.getId());
        t.setTokenHash(hash(token));
        t.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
        tokens.save(t);

        email.send(user.getUniversityEmail(), "Reset your TecSpace password",
                "Hello " + user.getFirstName() + ",\n\n"
                        + "Someone asked to reset the password for your TecSpace account. Open this link within 30 minutes:\n\n"
                        + frontendUrl + "/reset-password?token=" + token + "\n\n"
                        + "If this was not you, ignore this email. Your password stays the same.\n");
    }

    public void reset(String token, String newPassword) {
        PasswordResetToken t = tokens.findByTokenHash(hash(token))
                .filter(x -> !x.isUsed() && x.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This link is not valid any more. Ask for a new one."));
        User user = users.findById(t.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This link is not valid any more. Ask for a new one."));

        user.setPassword(encoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setFailedLogins(0);
        user.setLockedUntil(null);
        users.save(user);

        t.setUsed(true);
        tokens.save(t);
    }

    /** Thrown only inside request(); the controller turns it into the same quiet answer as everything else. */
    public static class TooSoon extends RuntimeException {}

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
