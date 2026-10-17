package com.tecspace.portal.service;

import com.tecspace.portal.dto.GradebookRow;
import com.tecspace.portal.dto.GradebookSave;
import com.tecspace.portal.dto.GradebookView;
import com.tecspace.portal.dto.MarkEntry;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.*;
import com.tecspace.portal.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A teacher's gradebook for one course. Coursework marks are shown as a guide. The teacher enters the final
 * percentage, and it is written into the student's grade record, so the grading scale, GPA, CGPA, trimester
 * locks and publishing all apply exactly as for grades entered by an administrator.
 */
@Service
public class GradebookService {

    private final FacultyService faculty;
    private final CourseOfferingService courseService;
    private final EnrollmentRepository enrollments;
    private final UserRepository users;
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final GradeService gradeService;
    private final ResultTermStateRepository termStates;

    public GradebookService(FacultyService faculty, CourseOfferingService courseService,
                            EnrollmentRepository enrollments, UserRepository users,
                            AssignmentRepository assignments, SubmissionRepository submissions,
                            GradeService gradeService, ResultTermStateRepository termStates) {
        this.faculty = faculty;
        this.courseService = courseService;
        this.enrollments = enrollments;
        this.users = users;
        this.assignments = assignments;
        this.submissions = submissions;
        this.gradeService = gradeService;
        this.termStates = termStates;
    }

    public GradebookView view(User teacher, String courseCode, String trimesterName) {
        faculty.requireAssigned(teacher.getId(), courseCode);
        CourseOffering course = courseService.get(courseCode);
        String trimester = (trimesterName == null || trimesterName.isBlank()) ? course.getTerm() : trimesterName.trim();

        List<Assignment> list = assignments.findByCourseCode(course.getCode());
        Map<String, Assignment> byId = list.stream().collect(Collectors.toMap(Assignment::getId, Function.identity()));
        Map<String, List<Submission>> byStudent = list.isEmpty() ? Map.of()
                : submissions.findByAssignmentIdIn(byId.keySet()).stream().collect(Collectors.groupingBy(Submission::getStudentId));

        List<GradebookRow> rows = new ArrayList<>();
        for (User u : enrolledStudents(course)) {
            double earned = 0;
            double possible = 0;
            int graded = 0;
            for (Submission s : byStudent.getOrDefault(u.getId(), List.of())) {
                Assignment a = byId.get(s.getAssignmentId());
                if (a == null || s.getMarks() == null) continue;
                earned += s.getMarks();
                possible += a.getMaxMarks();
                graded++;
            }
            Double pct = possible > 0 ? Math.round(earned / possible * 1000.0) / 10.0 : null;

            Course current = currentCourse(u.getId(), trimester, course.getCode());
            rows.add(new GradebookRow(u.getId(), fullName(u), earned, possible, pct, graded,
                    current == null ? null : current.getPercentage(),
                    current == null ? "" : (current.getGrade() == null ? "" : current.getGrade())));
        }
        boolean locked = termStates.findByTrimesterName(trimester).map(ResultTermState::isLocked).orElse(false);
        return new GradebookView(course.getCode(), course.getName(), trimester, locked, list.size(), rows);
    }

    public int save(User teacher, GradebookSave req) {
        faculty.requireAssigned(teacher.getId(), req.courseCode());
        CourseOffering course = courseService.get(req.courseCode());
        String trimester = req.trimesterName().trim();

        if (termStates.findByTrimesterName(trimester).map(ResultTermState::isLocked).orElse(false)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "\"" + trimester + "\" is locked. Ask an administrator to unlock it before changing grades.");
        }

        Set<String> allowed = enrollments
                .findByCourseCodeAndTermAndStatus(course.getCode(), course.getTerm(), Enrollment.ENROLLED)
                .stream().map(Enrollment::getStudentId).collect(Collectors.toSet());
        for (MarkEntry m : req.marks()) {
            if (!allowed.contains(m.studentId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A student on the list is not registered for " + course.getCode());
            }
        }

        for (MarkEntry m : req.marks()) {
            GradeRecord record = gradeService.get(m.studentId());
            List<Trimester> trimesters = record.getTrimesters();

            Trimester t = trimesters.stream().filter(x -> x.getTrimesterName().equals(trimester)).findFirst().orElse(null);
            if (t == null) {
                t = new Trimester();
                t.setTrimesterName(trimester);
                t.setCourses(new ArrayList<>());
                trimesters.add(t);
            }
            Course c = t.getCourses().stream().filter(x -> x.getCourseCode().equalsIgnoreCase(course.getCode())).findFirst().orElse(null);
            if (c == null) {
                c = new Course();
                c.setCourseCode(course.getCode());
                c.setCourseName(course.getName());
                c.setCreditHours(Math.max(course.getCreditHours(), 1));
                t.getCourses().add(c);
            }
            c.setPercentage(m.percentage());   // the letter is chosen from the grading scale by GradeService
            gradeService.update(m.studentId(), trimesters);
        }
        return req.marks().size();
    }

    // ---------------------------------------------------------------- helpers

    private Course currentCourse(String studentId, String trimester, String code) {
        return gradeService.get(studentId).getTrimesters().stream()
                .filter(t -> t.getTrimesterName().equals(trimester))
                .flatMap(t -> t.getCourses().stream())
                .filter(c -> c.getCourseCode().equalsIgnoreCase(code))
                .findFirst().orElse(null);
    }

    private List<User> enrolledStudents(CourseOffering course) {
        List<String> ids = enrollments.findByCourseCodeAndTermAndStatus(course.getCode(), course.getTerm(), Enrollment.ENROLLED)
                .stream().map(Enrollment::getStudentId).toList();
        List<User> list = new ArrayList<>(users.findAllById(ids));
        list.sort(Comparator.comparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private String fullName(User u) {
        return (u.getFirstName() + " " + u.getLastName()).trim();
    }
}
