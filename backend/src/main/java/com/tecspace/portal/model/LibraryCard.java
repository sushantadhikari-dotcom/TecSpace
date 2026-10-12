package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Data
@Document(collection = "library_cards")
public class LibraryCard {
    @Id
    private String id;

    @Indexed(unique = true)
    private String studentId;

    @Indexed(unique = true)
    private String cardNumber;

    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String qrCodePayload;
    private String status; // ACTIVE, EXPIRED, SUSPENDED
}
