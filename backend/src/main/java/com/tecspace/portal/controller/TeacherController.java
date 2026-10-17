package com.tecspace.portal.controller;

import com.tecspace.portal.dto.*;
import com.tecspace.portal.model.CourseOffering;
import com.tecspace.portal.model.Enrollment;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.CourseOfferingRepository;
import com.tecspace.portal.repository.EnrollmentRepository;
import com.tecspace.portal.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Everything a teacher can do, limited to the courses assigned to them. */
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final UserService userService;
    private final FacultyService faculty;
    private final CourseOfferingRepository courses;
    private final CourseOfferingService courseService;
    private final EnrollmentRepository enrollments;
    private final TimetableService timetable;
    private final AttendanceService attendance;
    private final NotificationService notifications;

    public TeacherController(UserService userService, FacultyService faculty, CourseOfferingRepository courses,
                             CourseOfferingService courseService, EnrollmentRepository enrollments,
                             TimetableService timetable, AttendanceService attendance,
                             NotificationService notifications) {
        this.userService = userService;
        this.faculty = faculty;
        this.courses = courses;
        this.courseService = courseService;
        this.enrollments = enrollments;
        this.timetable = timetable;
        this.attendance = attendance;
        this.notifications = notifications;
    }

    @GetMapping("/courses")
    public List<TeacherCourse> myCourses(Authentication auth) {
        User me = userService.getByEmail(auth.getName());
        Set<String> codes = faculty.codesFor(me.getId());
        List<ClassEntry> slots = timetable.forCourses(codes);

        return codes.stream().map(courses::findByCode).filter(java.util.Optional::isPresent).map(java.util.Optional::get)
                .map(c -> new TeacherCourse(c.getCode(), c.getName(), c.getDepartment(), c.getCreditHours(), c.getCapacity(),
                        (int) enrollments.countByCourseCodeAndTermAndStatus(c.getCode(), c.getTerm(), Enrollment.ENROLLED),
                        slots.stream().filter(s -> s.courseCode().equals(c.getCode()))
                                .map(s -> s.day() + " " + s.startTime() + "-" + s.endTime() + " · Room " + s.room()).toList()))
                .toList();
    }

    @GetMapping("/timetable")
    public List<ClassEntry> myTimetable(Authentication auth) {
        return timetable.forCourses(faculty.codesFor(userService.getByEmail(auth.getName()).getId()));
    }

    @GetMapping("/attendance/roster")
    public List<RosterRow> roster(Authentication auth, @RequestParam String courseCode,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        faculty.requireAssigned(userService.getByEmail(auth.getName()).getId(), courseCode);
        return attendance.roster(courseCode, date);
    }

    @PutMapping("/attendance")
    public Map<String, Object> saveAttendance(Authentication auth, @Valid @RequestBody AttendanceSaveRequest req) {
        faculty.requireAssigned(userService.getByEmail(auth.getName()).getId(), req.courseCode());
        return Map.of("saved", attendance.save(req));
    }

    @GetMapping("/attendance/report")
    public Map<String, Object> report(Authentication auth, @RequestParam(defaultValue = "false") boolean flaggedOnly) {
        Set<String> codes = faculty.codesFor(userService.getByEmail(auth.getName()).getId());
        return Map.of("threshold", attendance.threshold(),
                "rows", attendance.report(flaggedOnly, null).stream().filter(r -> codes.contains(r.courseCode())).toList());
    }

    /** Message every student registered in one of the teacher's courses. */
    @PostMapping("/messages")
    public Map<String, Object> message(Authentication auth, @Valid @RequestBody TeacherMessageRequest req) {
        User me = userService.getByEmail(auth.getName());
        faculty.requireAssigned(me.getId(), req.courseCode());
        CourseOffering course = courseService.get(req.courseCode());

        List<String> students = enrollments
                .findByCourseCodeAndTermAndStatus(course.getCode(), course.getTerm(), Enrollment.ENROLLED)
                .stream().map(Enrollment::getStudentId).toList();
        if (students.isEmpty()) {
            throw new com.tecspace.portal.exception.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Nobody is registered for " + course.getCode() + " yet");
        }
        notifications.notifyAll(students, "MESSAGE", "[" + course.getCode() + "] " + req.subject().trim(),
                req.body().trim(), (me.getFirstName() + " " + me.getLastName()).trim());
        return Map.of("sentTo", students.size());
    }
}
