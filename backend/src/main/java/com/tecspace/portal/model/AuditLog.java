package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private String id;

    @Indexed
    private Instant timestamp = Instant.now();

    private String actor;     // email, or "anonymous"
    private String role;
    private String method;    // POST, PUT, DELETE
    private String path;
    private int status;       // HTTP status returned
    private String ip;
}
