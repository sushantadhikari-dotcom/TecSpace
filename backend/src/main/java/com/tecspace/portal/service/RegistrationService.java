package com.tecspace.portal.service;

import com.tecspace.portal.dto.DirectEnrollRequest;
import com.tecspace.portal.dto.RegistrationDecision;
import com.tecspace.portal.dto.RegistrationRow;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.CourseOffering;
import com.tecspace.portal.model.Enrollment;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.CourseOfferingRepository;
import com.tecspace.portal.repository.EnrollmentRepository;
import com.tecspace.portal.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The administration's side of course registration. */
@Service
public class RegistrationService {

    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final CourseOfferingRepository courses;
    private final CourseOfferingService courseService;
    private final UserService userService;
    private final EnrollmentRules rules;
    private final NotificationService notifications;

    public RegistrationService(EnrollmentRepository enrollments, UserRepository users,
                               CourseOfferingRepository courses, CourseOfferingService courseService,
                               UserService userService, EnrollmentRules rules, NotificationService notifications) {
        this.enrollments = enrollments;
        this.users = users;
        this.courses = courses;
        this.courseService = courseService;
        this.userService = userService;
        this.rules = rules;
        this.notifications = notifications;
    }

    /** status = PENDING, ENROLLED, REJECTED, or empty for everything of the current term except withdrawn ones. */
    public List<RegistrationRow> list(String status) {
        String term = courseService.term();
        List<Enrollment> rows = (status == null || status.isBlank())
                ? enrollments.findAll().stream()
                .filter(e -> term.equals(e.getTerm()) && !Enrollment.DROPPED.equals(e.getStatus()))
                .sorted(Comparator.comparing(Enrollment::getEnrolledAt).reversed()).toList()
                : enrollments.findByStatusOrderByEnrolledAtDesc(status).stream()
                .filter(e -> term.equals(e.getTerm())).toList();

        Map<String, User> students = users.findAllById(rows.stream().map(Enrollment::getStudentId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        Map<String, CourseOffering> byCode = courses.findAll().stream()
                .collect(Collectors.toMap(CourseOffering::getCode, Function.identity(), (a, b) -> a));

        List<RegistrationRow> out = new ArrayList<>();
        for (Enrollment e : rows) {
            User u = students.get(e.getStudentId());
            CourseOffering c = byCode.get(e.getCourseCode());
            if (u == null || c == null) continue;   // the student or the course no longer exists

            boolean waiting = Enrollment.PENDING.equals(e.getStatus());
            out.add(new RegistrationRow(e.getId(), u.getId(), (u.getFirstName() + " " + u.getLastName()).trim(),
                    u.getFaculty(), c.getCode(), c.getName(), e.getStatus(), e.getEnrolledAt(), e.getDecidedBy(),
                    e.getNote() == null ? "" : e.getNote(),
                    Math.max(c.getCapacity() - rules.seatsTaken(c), 0), c.getCapacity(),
                    waiting ? rules.missingPrerequisites(u.getId(), c) : List.of(),
                    waiting ? rules.clash(u.getId(), c) : null));
        }
        return out;
    }

    public void decide(String id, String adminEmail, RegistrationDecision d) {
        Enrollment e = enrollments.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!Enrollment.PENDING.equals(e.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This request was already decided or withdrawn");
        }
        String note = d.note() == null ? "" : d.note().trim();

        if (!d.approve()) {
            e.setStatus(Enrollment.REJECTED);
            e.setNote(note);
            e.setDecidedBy(adminEmail);
            e.setDecidedAt(Instant.now());
            enrollments.save(e);
            notifications.notify(e.getStudentId(), "SYSTEM", "Course request declined: " + e.getCourseCode(),
                    "The administration did not approve your request." + (note.isEmpty() ? "" : " Note: " + note), null);
            return;
        }

        CourseOffering course = courseService.get(e.getCourseCode());
        if (!course.getTerm().equals(e.getTerm())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This request is for a previous term");
        }
        checkAll(e.getStudentId(), course, d.overridePrerequisites());

        approve(e, adminEmail, note);
    }

    /** The administration registers a student directly, with the same checks. */
    public void directEnroll(String adminEmail, DirectEnrollRequest r) {
        User student = userService.getStudentById(r.studentId());
        CourseOffering course = courseService.get(r.courseCode());
        String term = course.getTerm();

        List<Enrollment> open = enrollments.findByStudentIdAndCourseCodeAndTermAndStatusIn(
                student.getId(), course.getCode(), term, List.of(Enrollment.ENROLLED, Enrollment.PENDING));
        if (open.stream().anyMatch(x -> Enrollment.ENROLLED.equals(x.getStatus()))) {
            throw new ApiException(HttpStatus.CONFLICT, "This student is already registered for " + course.getCode());
        }
        checkAll(student.getId(), course, r.overridePrerequisites());

        Enrollment e = open.isEmpty() ? new Enrollment() : open.get(0);
        e.setStudentId(student.getId());
        e.setCourseCode(course.getCode());
        e.setTerm(term);
        approve(e, adminEmail, "");
    }

    // ---------------------------------------------------------------- helpers

    private void checkAll(String studentId, CourseOffering course, boolean overridePrerequisites) {
        if (!overridePrerequisites) {
            List<String> missing = rules.missingPrerequisites(studentId, course);
            if (!missing.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Missing prerequisite: " + String.join(", ", missing)
                        + ". Tick \"approve even if prerequisites are missing\" to make an exception.");
            }
        }
        if (rules.seatsTaken(course) >= course.getCapacity()) {
            throw new ApiException(HttpStatus.CONFLICT, course.getCode() + " is full. Raise the seat limit in Courses first.");
        }
        String clash = rules.clash(studentId, course);
        if (clash != null) throw new ApiException(HttpStatus.CONFLICT, clash);
    }

    private void approve(Enrollment e, String adminEmail, String note) {
        e.setStatus(Enrollment.ENROLLED);
        e.setEnrolledAt(Instant.now());
        e.setDecidedAt(Instant.now());
        e.setDecidedBy(adminEmail);
        e.setNote(note);
        enrollments.save(e);
        notifications.notify(e.getStudentId(), "SYSTEM", "Registered: " + e.getCourseCode(),
                "The administration approved your registration. You can now open the syllabus and see the class in your timetable.", null);
    }
}
