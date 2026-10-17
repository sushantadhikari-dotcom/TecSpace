package com.tecspace.portal.controller;

import com.tecspace.portal.dto.CertificateView;
import com.tecspace.portal.dto.InstructorMessageRequest;
import com.tecspace.portal.dto.RequestCreate;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.CourseOffering;
import com.tecspace.portal.model.Enrollment;
import com.tecspace.portal.model.StudentRequest;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.EnrollmentRepository;
import com.tecspace.portal.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Student requests (certificates, leave...) and messages to course instructors. */
@RestController
@RequestMapping("/api/student")
public class StudentServicesController {

    private final UserService userService;
    private final RequestService requests;
    private final CourseOfferingService courseService;
    private final EnrollmentRepository enrollments;
    private final FacultyService faculty;
    private final NotificationService notifications;

    public StudentServicesController(UserService userService, RequestService requests,
                                     CourseOfferingService courseService, EnrollmentRepository enrollments,
                                     FacultyService faculty, NotificationService notifications) {
        this.userService = userService;
        this.requests = requests;
        this.courseService = courseService;
        this.enrollments = enrollments;
        this.faculty = faculty;
        this.notifications = notifications;
    }

    @GetMapping("/requests")
    public List<StudentRequest> mine(Authentication auth) {
        return requests.mine(userService.getByEmail(auth.getName()).getId());
    }

    @PostMapping("/requests")
    public ResponseEntity<StudentRequest> create(Authentication auth, @Valid @RequestBody RequestCreate req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(requests.create(userService.getByEmail(auth.getName()), req));
    }

    @GetMapping("/requests/{id}/certificate")
    public CertificateView certificate(Authentication auth, @PathVariable String id) {
        return requests.certificate(userService.getByEmail(auth.getName()), id);
    }

    /** Goes to the teacher(s) assigned to the course. */
    @PostMapping("/instructor-messages")
    public ResponseEntity<Void> messageInstructor(Authentication auth, @Valid @RequestBody InstructorMessageRequest req) {
        User me = userService.getByEmail(auth.getName());
        CourseOffering course = courseService.get(req.courseCode());
        enrollments.findByStudentIdAndCourseCodeAndTermAndStatus(me.getId(), course.getCode(), course.getTerm(), Enrollment.ENROLLED)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "You are not registered for " + course.getCode()));

        List<String> teachers = faculty.teacherIdsFor(course.getCode());
        if (teachers.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No instructor has been assigned to " + course.getCode() + " yet");
        }
        String sender = (me.getFirstName() + " " + me.getLastName()).trim();
        notifications.notifyAll(teachers, "MESSAGE", "[" + course.getCode() + "] " + req.subject().trim(),
                req.body().trim(), sender);
        return ResponseEntity.noContent().build();
    }
}
