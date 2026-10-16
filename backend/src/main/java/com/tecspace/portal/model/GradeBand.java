package com.tecspace.portal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeBand {
    private String letter;
    private double minPercent;   // lowest percentage that earns this letter
    private double gradePoint;   // e.g. 4.0
}
