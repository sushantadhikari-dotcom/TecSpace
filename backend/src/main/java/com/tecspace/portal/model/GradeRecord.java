package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "grades")
public class GradeRecord {
    @Id
    private String id;

    @Indexed(unique = true)
    private String studentId;

    private double cgpa;
    private List<Trimester> trimesters = new ArrayList<>();
}
