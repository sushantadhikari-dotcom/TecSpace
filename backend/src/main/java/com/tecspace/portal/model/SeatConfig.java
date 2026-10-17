package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "seat_config")
@CompoundIndex(name = "faculty_session_unique", def = "{'faculty': 1, 'session': 1}", unique = true)
public class SeatConfig {
    @Id
    private String id;
    private String faculty;
    private String session;
    private int seats;
}
