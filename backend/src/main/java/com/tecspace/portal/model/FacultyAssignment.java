package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** Which course a teacher is responsible for. */
@Data
@Document(collection = "faculty_assignments")
@CompoundIndex(name = "teacher_course_unique", def = "{'teacherId': 1, 'courseCode': 1}", unique = true)
public class FacultyAssignment {
    @Id
    private String id;
    private String teacherId;
    private String courseCode;
}
