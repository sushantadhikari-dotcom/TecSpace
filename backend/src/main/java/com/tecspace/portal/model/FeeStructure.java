package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "fee_structures")
public class FeeStructure {
    @Id
    private String id;
    private String name;
    private String faculty;   // BSIT, BBA, MSCIT or ALL
    private String term;
    private List<FeeItem> items = new ArrayList<>();
}
