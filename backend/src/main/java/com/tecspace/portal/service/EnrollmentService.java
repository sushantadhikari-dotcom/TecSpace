package com.tecspace.portal.service;

import com.tecspace.portal.dto.ClassEntry;
import com.tecspace.portal.dto.CourseView;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.CourseOffering;
import com.tecspace.portal.model.Enrollment;
import com.tecspace.portal.model.GradeRecord;
import com.tecspace.portal.model.TimetableSlot;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.CourseOfferingRepository;
import com.tecspace.portal.repository.EnrollmentRepository;
import com.tecspace.portal.repository.GradeRepository;
import com.tecspace.portal.repository.TimetableSlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/** Course registration: add/drop with seat, prerequisite and timetable-clash checks. */
@Service
public class EnrollmentService {

    private final CourseOfferingService courseService;
    private final CourseOfferingRepository courses;
    private final EnrollmentRepository enrollments;
    private final TimetableSlotRepository slots;
    private final TimetableService timetable;
    private final GradeRepository grades;

    public EnrollmentService(CourseOfferingService courseService, CourseOfferingRepository courses,
                             EnrollmentRepository enrollments, TimetableSlotRepository slots,
                             TimetableService timetable, GradeRepository grades) {
        this.courseService = courseService;
        this.courses = courses;
        this.enrollments = enrollments;
        this.slots = slots;
        this.timetable = timetable;
        this.grades = grades;
    }

    // ---------------------------------------------------------------- views

    /** Every course offered this term, with seats left and whether this student is enrolled. */
    public List<CourseView> catalog(User student) {
        String term = courseService.term();
        Set<String> mine = enrolledCodes(student.getId());
        List<CourseOffering> offered = courses.findByTerm(term);
        return offered.stream().map(c -> view(c, mine.contains(c.getCode()))).toList();
    }

    public List<CourseView> myCourses(User student) {
        return catalog(student).stream().filter(CourseView::enrolled).toList();
    }

    public List<ClassEntry> myTimetable(User student) {
        return timetable.forCourses(enrolledCodes(student.getId()));
    }

    // ---------------------------------------------------------------- actions

    public CourseView register(User student, String courseCode) {
        String term = courseService.term();
        CourseOffering course = courseService.get(courseCode);
        if (!term.equals(course.getTerm())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This course is not open for registration this term");
        }

        Set<String> mine = enrolledCodes(student.getId());
        if (mine.contains(course.getCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "You are already registered for " + course.getCode());
        }

        // 1. Prerequisites
        Set<String> passed = passedCourseCodes(student.getId());
        List<String> missing = course.getPrerequisites().stream().filter(p -> !passed.contains(p)).toList();
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing prerequisite: " + String.join(", ", missing));
        }

        // 2. Seats
        long taken = enrollments.countByCourseCodeAndTermAndStatus(course.getCode(), term, Enrollment.ENROLLED);
        if (taken >= course.getCapacity()) {
            throw new ApiException(HttpStatus.CONFLICT, course.getCode() + " is full");
        }

        // 3. Timetable clash with the courses the student already takes
        List<TimetableSlot> wanted = slots.findByCourseCode(course.getCode());
        List<TimetableSlot> existing = mine.isEmpty() ? List.of() : slots.findByCourseCodeIn(mine);
        for (TimetableSlot a : wanted) {
            for (TimetableSlot b : existing) {
                if (a.getDay().equals(b.getDay())
                        && TimetableService.overlaps(a.getStartTime(), a.getEndTime(), b.getStartTime(), b.getEndTime())) {
                    throw new ApiException(HttpStatus.CONFLICT, course.getCode() + " clashes with " + b.getCourseCode()
                            + " on " + b.getDay() + " " + b.getStartTime() + "-" + b.getEndTime());
                }
            }
        }

        Enrollment e = new Enrollment();
        e.setStudentId(student.getId());
        e.setCourseCode(course.getCode());
        e.setTerm(term);
        enrollments.save(e);
        return view(course, true);
    }

    public void drop(User student, String courseCode) {
        String term = courseService.term();
        String code = courseCode.trim().toUpperCase();
        Enrollment e = enrollments
                .findByStudentIdAndCourseCodeAndTermAndStatus(student.getId(), code, term, Enrollment.ENROLLED)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You are not registered for " + code));
        e.setStatus(Enrollment.DROPPED);
        enrollments.save(e);
    }

    // ---------------------------------------------------------------- helpers

    public Set<String> enrolledCodes(String studentId) {
        return enrollments.findByStudentIdAndTermAndStatus(studentId, courseService.term(), Enrollment.ENROLLED)
                .stream().map(Enrollment::getCourseCode).collect(Collectors.toCollection(HashSet::new));
    }

    private Set<String> passedCourseCodes(String studentId) {
        Set<String> passed = new HashSet<>();
        Optional<GradeRecord> record = grades.findByStudentId(studentId);
        record.ifPresent(r -> r.getTrimesters().forEach(t -> t.getCourses().forEach(c -> {
            if (!"F".equalsIgnoreCase(c.getGrade())) passed.add(c.getCourseCode().trim().toUpperCase());
        })));
        return passed;
    }

    private CourseView view(CourseOffering c, boolean enrolled) {
        int taken = (int) enrollments.countByCourseCodeAndTermAndStatus(c.getCode(), c.getTerm(), Enrollment.ENROLLED);
        List<String> schedule = slots.findByCourseCode(c.getCode()).stream()
                .sorted(Comparator.comparingInt((TimetableSlot s) -> TimetableService.DAYS.indexOf(s.getDay()))
                        .thenComparing(TimetableSlot::getStartTime))
                .map(s -> s.getDay() + " " + s.getStartTime() + "-" + s.getEndTime() + " · Room " + s.getRoom())
                .toList();
        return new CourseView(c.getCode(), c.getName(), c.getDepartment(), c.getCreditHours(), c.getCapacity(),
                taken, Math.max(c.getCapacity() - taken, 0), c.getInstructorName(), c.getPrerequisites(),
                enrolled, schedule);
    }
}
