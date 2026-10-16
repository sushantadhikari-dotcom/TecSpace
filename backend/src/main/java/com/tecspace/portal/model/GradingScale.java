package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "grading_scale")
public class GradingScale {
    @Id
    private String id;
    private String name = "Standard 4.0";
    private List<GradeBand> bands = new ArrayList<>();
}
