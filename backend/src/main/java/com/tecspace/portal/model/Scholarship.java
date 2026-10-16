package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Document(collection = "scholarships")
public class Scholarship {
    public static final String ACTIVE = "ACTIVE";
    public static final String REVOKED = "REVOKED";

    @Id
    private String id;

    @Indexed
    private String studentId;

    private String name;
    private String type;          // PERCENT or FIXED
    private BigDecimal value;     // 25 (percent) or 5000 (amount)
    private BigDecimal amount;    // the money actually taken off the fees
    private String term;
    private String status = ACTIVE;
    private Instant awardedAt = Instant.now();
}
