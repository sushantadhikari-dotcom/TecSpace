package com.tecspace.portal.service;

import com.tecspace.portal.model.CourseOffering;
import com.tecspace.portal.model.Enrollment;
import com.tecspace.portal.model.GradeRecord;
import com.tecspace.portal.model.TimetableSlot;
import com.tecspace.portal.repository.EnrollmentRepository;
import com.tecspace.portal.repository.GradeRepository;
import com.tecspace.portal.repository.TimetableSlotRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** The checks shared by a student's request and the administration's approval. */
@Component
public class EnrollmentRules {

    private final GradeRepository grades;
    private final EnrollmentRepository enrollments;
    private final TimetableSlotRepository slots;

    public EnrollmentRules(GradeRepository grades, EnrollmentRepository enrollments, TimetableSlotRepository slots) {
        this.grades = grades;
        this.enrollments = enrollments;
        this.slots = slots;
    }

    /** Courses the student passed (any grade except F). */
    public Set<String> passedCodes(String studentId) {
        Set<String> passed = new HashSet<>();
        Optional<GradeRecord> record = grades.findByStudentId(studentId);
        record.ifPresent(r -> r.getTrimesters().forEach(t -> t.getCourses().forEach(c -> {
            if (c.getGrade() != null && !"F".equalsIgnoreCase(c.getGrade())) {
                passed.add(c.getCourseCode().trim().toUpperCase());
            }
        })));
        return passed;
    }

    public List<String> missingPrerequisites(String studentId, CourseOffering course) {
        Set<String> passed = passedCodes(studentId);
        return course.getPrerequisites().stream().filter(p -> !passed.contains(p)).toList();
    }

    /** Seats taken by APPROVED students only. Waiting requests do not hold a seat. */
    public int seatsTaken(CourseOffering course) {
        return (int) enrollments.countByCourseCodeAndTermAndStatus(course.getCode(), course.getTerm(), Enrollment.ENROLLED);
    }

    /** A message describing the clash with an already approved course, or null if there is none. */
    public String clash(String studentId, CourseOffering course) {
        Set<String> approved = enrollments
                .findByStudentIdAndTermAndStatus(studentId, course.getTerm(), Enrollment.ENROLLED)
                .stream().map(Enrollment::getCourseCode).collect(Collectors.toSet());
        approved.remove(course.getCode());
        if (approved.isEmpty()) return null;

        List<TimetableSlot> wanted = slots.findByCourseCode(course.getCode());
        List<TimetableSlot> existing = slots.findByCourseCodeIn(approved);
        for (TimetableSlot a : wanted) {
            for (TimetableSlot b : existing) {
                if (a.getDay().equals(b.getDay())
                        && TimetableService.overlaps(a.getStartTime(), a.getEndTime(), b.getStartTime(), b.getEndTime())) {
                    return course.getCode() + " clashes with " + b.getCourseCode() + " on " + b.getDay() + " "
                            + b.getStartTime() + "-" + b.getEndTime();
                }
            }
        }
        return null;
    }
}
