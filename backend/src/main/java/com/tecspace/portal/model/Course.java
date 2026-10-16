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
    /** Letter grade. May be left empty if a percentage is given: the letter is then worked out from the grading scale. */
    private String grade;
    /** Optional marks out of 100. */
    private Double percentage;
    @Min(1)
    private int creditHours;
}
