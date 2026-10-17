package com.tecspace.portal.dto;

import java.util.List;

public record TeacherCourse(String code, String name, String department, int creditHours,
                            int capacity, int enrolledCount, List<String> schedule) {}
