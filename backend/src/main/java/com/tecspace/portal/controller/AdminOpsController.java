package com.tecspace.portal.controller;

import com.tecspace.portal.dto.*;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.Enrollment;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.EnrollmentRepository;
import com.tecspace.portal.repository.UserRepository;
import com.tecspace.portal.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Phase 2 admin endpoints: attendance, exam timetable and messaging. */
@RestController
@RequestMapping("/api/admin")
public class AdminOpsController {

    private final AttendanceService attendanceService;
    private final ExamService examService;
    private final NotificationService notifications;
    private final UserService userService;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollments;
    private final CourseOfferingService courseService;

    public AdminOpsController(AttendanceService attendanceService, ExamService examService,
                              NotificationService notifications, UserService userService,
                              UserRepository userRepository, EnrollmentRepository enrollments,
                              CourseOfferingService courseService) {
        this.attendanceService = attendanceService;
        this.examService = examService;
        this.notifications = notifications;
        this.userService = userService;
        this.userRepository = userRepository;
        this.enrollments = enrollments;
        this.courseService = courseService;
    }

    // ---- attendance
    @GetMapping("/attendance/roster")
    public List<RosterRow> roster(@RequestParam String courseCode,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.roster(courseCode, date);
    }

    @PutMapping("/attendance")
    public Map<String, Object> saveAttendance(@Valid @RequestBody AttendanceSaveRequest req) {
        return Map.of("saved", attendanceService.save(req));
    }

    @GetMapping("/attendance/report")
    public Map<String, Object> report(@RequestParam(defaultValue = "false") boolean flaggedOnly,
                                      @RequestParam(required = false) Double threshold) {
        return Map.of(
                "threshold", threshold == null ? attendanceService.threshold() : threshold,
                "rows", attendanceService.report(flaggedOnly, threshold));
    }

    // ---- exams
    @GetMapping("/exams")
    public List<ExamEntry> exams() {
        return examService.all();
    }

    @PostMapping("/exams")
    public ResponseEntity<ExamEntry> addExam(@Valid @RequestBody ExamRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(examService.add(req));
    }

    @DeleteMapping("/exams/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable String id) {
        examService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- messages
    @PostMapping("/messages")
    public Map<String, Object> send(Authentication auth, @Valid @RequestBody AdminMessageRequest req) {
        User admin = userService.getByEmail(auth.getName());
        String sender = (admin.getFirstName() + " " + admin.getLastName()).trim();

        List<String> recipients;
        switch (req.target()) {
            case "STUDENT" -> {
                if (req.studentId() == null || req.studentId().isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a student");
                }
                recipients = List.of(userService.getStudentById(req.studentId()).getId());
            }
            case "COURSE" -> {
                if (req.courseCode() == null || req.courseCode().isBlank()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a course");
                }
                var course = courseService.get(req.courseCode());
                recipients = enrollments
                        .findByCourseCodeAndTermAndStatus(course.getCode(), course.getTerm(), Enrollment.ENROLLED)
                        .stream().map(Enrollment::getStudentId).toList();
            }
            default -> recipients = userRepository.findByRole("ROLE_STUDENT").stream().map(User::getId).toList();
        }
        if (recipients.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "There is nobody to send this message to");
        }
        notifications.notifyAll(recipients, "MESSAGE", req.subject().trim(), req.body().trim(), sender);
        return Map.of("sentTo", recipients.size());
    }
}
