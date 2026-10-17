package com.tecspace.portal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalInfo {
    private String bloodGroup = "";
    private String allergies = "";
    private String conditions = "";
    private String medications = "";
    private String notes = "";
}
