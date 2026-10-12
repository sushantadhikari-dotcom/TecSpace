package com.tecspace.portal.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Trimester {
    @NotBlank
    private String trimesterName;
    private double gpa;   // calculated by the server
    @Valid
    private List<Course> courses = new ArrayList<>();
}
