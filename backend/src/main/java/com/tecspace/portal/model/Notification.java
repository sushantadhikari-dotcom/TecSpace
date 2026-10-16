package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;

    @Indexed
    private String userId;       // recipient

    private String type;         // ATTENDANCE, EXAM, MESSAGE, SYSTEM
    private String title;
    private String message;
    private String senderName;   // set for MESSAGE
    private boolean read = false;
    private Instant createdAt = Instant.now();
}
