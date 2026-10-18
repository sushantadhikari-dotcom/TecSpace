package com.tecspace.portal.dto;

import java.util.List;

public record SyllabusView(String code, String name, String department, int creditHours, String instructorName,
                           List<String> prerequisites, List<String> schedule, String syllabus, String syllabusUrl) {}
