package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Only a hash of the reset token is stored, so a copy of the database cannot be used to reset accounts. */
@Data
@Document(collection = "password_reset_tokens")
public class PasswordResetToken {
    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed(unique = true)
    private String tokenHash;

    private Instant expiresAt;
    private boolean used;

    @Indexed(expireAfterSeconds = 86400)   // MongoDB removes old tokens by itself after a day
    private Instant createdAt = Instant.now();
}
