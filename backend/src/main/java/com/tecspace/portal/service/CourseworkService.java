package com.tecspace.portal.service;

import com.tecspace.portal.dto.*;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.*;
import com.tecspace.portal.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Assignments (teachers) and submissions (students), with marks and feedback. */
@Service
public class CourseworkService {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final CourseOfferingRepository courses;
    private final CourseOfferingService courseService;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final FacultyService faculty;
    private final NotificationService notifications;

    public CourseworkService(AssignmentRepository assignments, SubmissionRepository submissions,
                             CourseOfferingRepository courses, CourseOfferingService courseService,
                             EnrollmentRepository enrollments, UserRepository users, FacultyService faculty,
                             NotificationService notifications) {
        this.assignments = assignments;
        this.submissions = submissions;
        this.courses = courses;
        this.courseService = courseService;
        this.enrollments = enrollments;
        this.users = users;
        this.faculty = faculty;
        this.notifications = notifications;
    }

    // ================================================================ teacher

    public List<AssignmentView> forTeacher(User teacher) {
        Set<String> codes = faculty.codesFor(teacher.getId());
        if (codes.isEmpty()) return List.of();
        Map<String, String> names = courseNames();
        return assignments.findByCourseCodeIn(codes).stream()
                .sorted(Comparator.comparing(Assignment::getDueAt).reversed())
                .map(a -> teacherView(a, names)).toList();
    }

    public AssignmentView create(User teacher, AssignmentRequest r) {
        faculty.requireAssigned(teacher.getId(), r.courseCode());
        CourseOffering course = courseService.get(r.courseCode());
        if (!r.dueAt().isAfter(LocalDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The due date must be in the future");
        }
        Assignment a = new Assignment();
        a.setCourseCode(course.getCode());
        apply(a, r);
        a.setCreatedBy(teacher.getId());
        a = assignments.save(a);

        notifications.notifyAll(enrolledIds(course), "SYSTEM", "New assignment: " + course.getCode() + " · " + a.getTitle(),
                "Due " + a.getDueAt().format(WHEN) + ", " + (int) a.getMaxMarks() + " marks.", null);
        return teacherView(a, courseNames());
    }

    public AssignmentView update(User teacher, String id, AssignmentRequest r) {
        Assignment a = ownedBy(teacher, id);
        if (r.maxMarks() != a.getMaxMarks() && submissions.findByAssignmentId(id).stream().anyMatch(s -> s.getMarks() != null)) {
            throw new ApiException(HttpStatus.CONFLICT, "Marks were already given, so the maximum marks cannot change");
        }
        apply(a, r);
        return teacherView(assignments.save(a), courseNames());
    }

    public void delete(User teacher, String id) {
        Assignment a = ownedBy(teacher, id);
        if (submissions.countByAssignmentId(id) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Students have already handed this in, so it cannot be deleted");
        }
        assignments.delete(a);
    }

    /** Every student registered in the course, with what they handed in (if anything). */
    public List<SubmissionRow> submissions(User teacher, String assignmentId) {
        Assignment a = ownedBy(teacher, assignmentId);
        CourseOffering course = courseService.get(a.getCourseCode());
        Map<String, Submission> byStudent = submissions.findByAssignmentId(assignmentId).stream()
                .collect(Collectors.toMap(Submission::getStudentId, Function.identity(), (x, y) -> x));

        List<SubmissionRow> rows = new ArrayList<>();
        for (User u : enrolledStudents(course)) {
            Submission s = byStudent.get(u.getId());
            rows.add(s == null
                    ? new SubmissionRow(u.getId(), fullName(u), "NOT_SUBMITTED", "", "", null, false, null, "")
                    : new SubmissionRow(u.getId(), fullName(u), statusOf(s), s.getText(), s.getLink(),
                    s.getSubmittedAt(), s.isLate(), s.getMarks(), s.getFeedback()));
        }
        return rows;
    }

    public SubmissionRow grade(User teacher, String assignmentId, GradeSubmissionRequest r) {
        Assignment a = ownedBy(teacher, assignmentId);
        if (r.marks() > a.getMaxMarks()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Marks cannot be more than " + a.getMaxMarks());
        }
        Submission s = submissions.findByAssignmentIdAndStudentId(assignmentId, r.studentId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "This student has not handed anything in"));
        s.setMarks(r.marks());
        s.setFeedback(r.feedback() == null ? "" : r.feedback().trim());
        s.setGradedAt(Instant.now());
        s.setGradedBy(teacher.getId());
        s = submissions.save(s);

        notifications.notify(s.getStudentId(), "SYSTEM", "Assignment marked: " + a.getTitle(),
                "You scored " + trim(s.getMarks()) + " out of " + trim(a.getMaxMarks()) + "."
                        + (s.getFeedback().isEmpty() ? "" : " Feedback: " + s.getFeedback()), null);

        User student = users.findById(s.getStudentId()).orElseThrow();
        return new SubmissionRow(student.getId(), fullName(student), statusOf(s), s.getText(), s.getLink(),
                s.getSubmittedAt(), s.isLate(), s.getMarks(), s.getFeedback());
    }

    // ================================================================ student

    public List<StudentAssignmentView> forStudent(User student) {
        Set<String> codes = enrollments
                .findByStudentIdAndTermAndStatus(student.getId(), courseService.term(), Enrollment.ENROLLED)
                .stream().map(Enrollment::getCourseCode).collect(Collectors.toSet());
        if (codes.isEmpty()) return List.of();

        Map<String, String> names = courseNames();
        Map<String, Submission> mine = submissions.findByStudentId(student.getId()).stream()
                .collect(Collectors.toMap(Submission::getAssignmentId, Function.identity(), (x, y) -> x));

        return assignments.findByCourseCodeIn(codes).stream()
                .sorted(Comparator.comparing(Assignment::getDueAt))
                .map(a -> studentView(a, names, mine.get(a.getId()))).toList();
    }

    public StudentAssignmentView submit(User student, String assignmentId, SubmitRequest r) {
        Assignment a = assignments.findById(assignmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
        CourseOffering course = courseService.get(a.getCourseCode());
        enrollments.findByStudentIdAndCourseCodeAndTermAndStatus(student.getId(), course.getCode(), course.getTerm(), Enrollment.ENROLLED)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not registered for " + course.getCode()));

        String text = r.text() == null ? "" : r.text().trim();
        String link = r.link() == null ? "" : r.link().trim();
        if (text.isEmpty() && link.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Write your answer or add a link to your work");
        }
        if (!link.isEmpty() && !(link.startsWith("http://") || link.startsWith("https://"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The link must start with http:// or https://");
        }

        Submission s = submissions.findByAssignmentIdAndStudentId(assignmentId, student.getId()).orElseGet(Submission::new);
        if (s.getMarks() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "This work was already marked, so it cannot be changed");
        }
        s.setAssignmentId(assignmentId);
        s.setStudentId(student.getId());
        s.setText(text);
        s.setLink(link);
        s.setSubmittedAt(Instant.now());
        s.setLate(LocalDateTime.now().isAfter(a.getDueAt()));
        s = submissions.save(s);
        return studentView(a, courseNames(), s);
    }

    // ================================================================ helpers

    private Assignment ownedBy(User teacher, String id) {
        Assignment a = assignments.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
        faculty.requireAssigned(teacher.getId(), a.getCourseCode());
        return a;
    }

    private void apply(Assignment a, AssignmentRequest r) {
        a.setTitle(r.title().trim());
        a.setDescription(r.description() == null ? "" : r.description().trim());
        a.setDueAt(r.dueAt());
        a.setMaxMarks(r.maxMarks());
    }

    private AssignmentView teacherView(Assignment a, Map<String, String> names) {
        List<Submission> subs = submissions.findByAssignmentId(a.getId());
        CourseOffering course = courses.findByCode(a.getCourseCode()).orElse(null);
        int students = course == null ? 0 : enrolledIds(course).size();
        return new AssignmentView(a.getId(), a.getCourseCode(), names.getOrDefault(a.getCourseCode(), a.getCourseCode()),
                a.getTitle(), a.getDescription(), a.getDueAt(), a.getMaxMarks(), subs.size(),
                (int) subs.stream().filter(s -> s.getMarks() != null).count(), students);
    }

    private StudentAssignmentView studentView(Assignment a, Map<String, String> names, Submission s) {
        String status = s == null ? "NOT_SUBMITTED" : statusOf(s);
        boolean overdue = s == null && LocalDateTime.now().isAfter(a.getDueAt());
        return new StudentAssignmentView(a.getId(), a.getCourseCode(), names.getOrDefault(a.getCourseCode(), a.getCourseCode()),
                a.getTitle(), a.getDescription(), a.getDueAt(), a.getMaxMarks(), status, overdue,
                s == null ? "" : s.getText(), s == null ? "" : s.getLink(), s == null ? null : s.getSubmittedAt(),
                s == null ? null : s.getMarks(), s == null ? "" : s.getFeedback());
    }

    private String statusOf(Submission s) {
        if (s.getMarks() != null) return "GRADED";
        return s.isLate() ? "LATE" : "SUBMITTED";
    }

    private Map<String, String> courseNames() {
        return courses.findAll().stream()
                .collect(Collectors.toMap(CourseOffering::getCode, CourseOffering::getName, (x, y) -> x));
    }

    private List<String> enrolledIds(CourseOffering course) {
        return enrollments.findByCourseCodeAndTermAndStatus(course.getCode(), course.getTerm(), Enrollment.ENROLLED)
                .stream().map(Enrollment::getStudentId).toList();
    }

    private List<User> enrolledStudents(CourseOffering course) {
        List<User> list = new ArrayList<>(users.findAllById(enrolledIds(course)));
        list.sort(Comparator.comparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private String fullName(User u) {
        return (u.getFirstName() + " " + u.getLastName()).trim();
    }

    private String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }
}
