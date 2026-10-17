package com.tecspace.portal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDocument {
    private String name;
    private String status = "PENDING";   // PENDING, VERIFIED, REJECTED
    private String note = "";
}
