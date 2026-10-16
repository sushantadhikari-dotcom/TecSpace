package com.tecspace.portal.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceEntry {
    private String studentId;
    private String status;   // PRESENT, ABSENT, LATE, EXCUSED
}
