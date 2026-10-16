package com.tecspace.portal.controller;

import com.tecspace.portal.dto.AttendanceStat;
import com.tecspace.portal.dto.ExamEntry;
import com.tecspace.portal.service.AttendanceService;
import com.tecspace.portal.service.ExamService;
import com.tecspace.portal.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Phase 2 student endpoints: attendance and exam schedule. */
@RestController
@RequestMapping("/api/student")
public class StudentOpsController {

    private final AttendanceService attendanceService;
    private final ExamService examService;
    private final UserService userService;

    public StudentOpsController(AttendanceService attendanceService, ExamService examService, UserService userService) {
        this.attendanceService = attendanceService;
        this.examService = examService;
        this.userService = userService;
    }

    @GetMapping("/attendance")
    public Map<String, Object> attendance(Authentication auth) {
        List<AttendanceStat> rows = attendanceService.forStudent(userService.getByEmail(auth.getName()));
        return Map.of("threshold", attendanceService.threshold(), "courses", rows);
    }

    @GetMapping("/exams")
    public List<ExamEntry> exams(Authentication auth) {
        return examService.forStudent(userService.getByEmail(auth.getName()));
    }
}
