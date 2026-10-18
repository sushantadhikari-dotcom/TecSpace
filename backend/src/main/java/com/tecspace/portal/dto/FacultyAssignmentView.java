package com.tecspace.portal.dto;

import java.util.List;

public record FacultyAssignmentView(String teacherId, String teacherName, String email, List<String> courseCodes) {}