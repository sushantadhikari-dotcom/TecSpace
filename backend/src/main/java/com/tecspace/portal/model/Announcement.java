package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "announcements")
public class Announcement {
    @Id
    private String id;
    private String title;
    private String message;
    private String postedBy;
    private Instant createdAt = Instant.now();
}
