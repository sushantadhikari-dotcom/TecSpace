package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Lock / publish state of one trimester's results (matched by trimester name). */
@Data
@Document(collection = "result_terms")
public class ResultTermState {
    @Id
    private String id;

    @Indexed(unique = true)
    private String trimesterName;

    private boolean locked;
    private boolean published;
    private Instant updatedAt = Instant.now();
}
