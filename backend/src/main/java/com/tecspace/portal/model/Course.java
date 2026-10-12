package com.tecspace.portal.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Course {
    @NotBlank
    private String courseCode;
    @NotBlank
    private String courseName;
    @NotBlank
    private String grade;
    @Min(1)
    private int creditHours;
}
