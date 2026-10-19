package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Remembers which reminders were already sent so nobody gets the same one twice. */
@Data
@Document(collection = "reminder_log")
public class ReminderLog {
    @Id
    private String id;

    @Indexed(unique = true)
    private String key;

    private Instant sentAt = Instant.now();

    public ReminderLog() {}

    public ReminderLog(String key) {
        this.key = key;
    }
}
