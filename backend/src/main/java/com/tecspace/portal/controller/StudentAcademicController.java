package com.tecspace.portal.controller;

import com.tecspace.portal.dto.ClassEntry;
import com.tecspace.portal.dto.CourseView;
import com.tecspace.portal.dto.DashboardResponse;
import com.tecspace.portal.dto.EnrollRequest;
import com.tecspace.portal.model.Announcement;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.AnnouncementRepository;
import com.tecspace.portal.service.CourseOfferingService;
import com.tecspace.portal.service.EnrollmentService;
import com.tecspace.portal.service.PaymentService;
import com.tecspace.portal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentAcademicController {

    private final UserService userService;
    private final EnrollmentService enrollmentService;
    private final CourseOfferingService courseService;
    private final PaymentService paymentService;
    private final AnnouncementRepository announcements;

    public StudentAcademicController(UserService userService, EnrollmentService enrollmentService,
                                     CourseOfferingService courseService, PaymentService paymentService,
                                     AnnouncementRepository announcements) {
        this.userService = userService;
        this.enrollmentService = enrollmentService;
        this.courseService = courseService;
        this.paymentService = paymentService;
        this.announcements = announcements;
    }

    @GetMapping("/courses")
    public List<CourseView> catalog(Authentication auth) {
        return enrollmentService.catalog(userService.getByEmail(auth.getName()));
    }

    @GetMapping("/enrollments")
    public List<CourseView> myCourses(Authentication auth) {
        return enrollmentService.myCourses(userService.getByEmail(auth.getName()));
    }

    @PostMapping("/enrollments")
    public ResponseEntity<CourseView> register(Authentication auth, @Valid @RequestBody EnrollRequest req) {
        User me = userService.getByEmail(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.register(me, req.courseCode()));
    }

    @DeleteMapping("/enrollments/{code}")
    public ResponseEntity<Void> drop(Authentication auth, @PathVariable String code) {
        enrollmentService.drop(userService.getByEmail(auth.getName()), code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/timetable")
    public List<ClassEntry> timetable(Authentication auth) {
        return enrollmentService.myTimetable(userService.getByEmail(auth.getName()));
    }

    @GetMapping("/announcements")
    public List<Announcement> announcements() {
        return announcements.findTop20ByOrderByCreatedAtDesc();
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(Authentication auth) {
        User me = userService.getByEmail(auth.getName());
        List<CourseView> mine = enrollmentService.myCourses(me);
        String today = LocalDate.now().getDayOfWeek().name();
        List<ClassEntry> todayClasses = enrollmentService.myTimetable(me).stream()
                .filter(c -> c.day().equals(today)).toList();

        return new DashboardResponse(
                courseService.term(),
                paymentService.getRecord(me.getId()).getRemainingBalance(),
                mine.size(),
                mine.stream().mapToInt(CourseView::creditHours).sum(),
                todayClasses,
                announcements.findTop5ByOrderByCreatedAtDesc());
    }
}
