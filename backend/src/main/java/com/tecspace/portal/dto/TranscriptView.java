package com.tecspace.portal.dto;

import com.tecspace.portal.model.Course;

import java.time.LocalDate;
import java.util.List;

public record TranscriptView(
        String studentName, String email, String faculty, String session, LocalDate issuedOn,
        List<TermView> terms, double cgpa, int totalCredits
) {
    public record TermView(String name, double gpa, int credits, List<Course> courses) {}
}
