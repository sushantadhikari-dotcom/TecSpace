package com.tecspace.portal.service;

import com.tecspace.portal.dto.ClassEntry;
import com.tecspace.portal.dto.CourseView;
import com.tecspace.portal.dto.SyllabusView;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.CourseOffering;
import com.tecspace.portal.model.Enrollment;
import com.tecspace.portal.model.TimetableSlot;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.CourseOfferingRepository;
import com.tecspace.portal.repository.EnrollmentRepository;
import com.tecspace.portal.repository.TimetableSlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Course registration. A student can only ASK for a course (PENDING).
 * The administration approves it (see RegistrationService). Only approved courses count for the timetable,
 * attendance, assignments, exams and the syllabus.
 */
@Service
public class EnrollmentService {

    private final CourseOfferingService courseService;
    private final CourseOfferingRepository courses;
    private final EnrollmentRepository enrollments;
    private final TimetableSlotRepository slots;
    private final TimetableService timetable;
    private final EnrollmentRules rules;
    private final NotificationService notifications;

    public EnrollmentService(CourseOfferingService courseService, CourseOfferingRepository courses,
                             EnrollmentRepository enrollments, TimetableSlotRepository slots,
                             TimetableService timetable, EnrollmentRules rules, NotificationService notifications) {
        this.courseService = courseService;
        this.courses = courses;
        this.enrollments = enrollments;
        this.slots = slots;
        this.timetable = timetable;
        this.rules = rules;
        this.notifications = notifications;
    }

    // ---------------------------------------------------------------- views

    /** Every course offered this term with the student's own status for it. */
    public List<CourseView> catalog(User student) {
        String term = courseService.term();
        Map<String, Enrollment> best = bestByCourse(student.getId(), term);
        return courses.findByTerm(term).stream().map(c -> {
            Enrollment e = best.get(c.getCode());
            String status = (e == null || Enrollment.DROPPED.equals(e.getStatus())) ? "NONE" : e.getStatus();
            String note = (e != null && Enrollment.REJECTED.equals(e.getStatus()) && e.getNote() != null) ? e.getNote() : "";
            return view(c, status, note);
        }).toList();
    }

    /** Only approved courses. */
    public List<CourseView> myCourses(User student) {
        return catalog(student).stream().filter(CourseView::enrolled).toList();
    }

    public List<ClassEntry> myTimetable(User student) {
        return timetable.forCourses(enrolledCodes(student.getId()));
    }

    /** The syllabus is visible only after the administration approved the registration. */
    public SyllabusView syllabus(User student, String courseCode) {
        CourseOffering c = courseService.get(courseCode);
        boolean approved = enrollments
                .findByStudentIdAndCourseCodeAndTermAndStatus(student.getId(), c.getCode(), c.getTerm(), Enrollment.ENROLLED)
                .isPresent();
        if (!approved) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "You can open the syllabus of " + c.getCode() + " after the administration approves your registration");
        }
        return new SyllabusView(c.getCode(), c.getName(), c.getDepartment(), c.getCreditHours(), c.getInstructorName(),
                c.getPrerequisites(), schedule(c.getCode()), c.getSyllabus(), c.getSyllabusUrl());
    }

    // ---------------------------------------------------------------- actions

    /** Sends a registration REQUEST. Nothing is registered until the administration approves it. */
    public CourseView register(User student, String courseCode) {
        String term = courseService.term();
        CourseOffering course = courseService.get(courseCode);
        if (!term.equals(course.getTerm())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This course is not open for registration this term");
        }

        List<Enrollment> open = enrollments.findByStudentIdAndCourseCodeAndTermAndStatusIn(
                student.getId(), course.getCode(), term, List.of(Enrollment.ENROLLED, Enrollment.PENDING));
        if (!open.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, Enrollment.ENROLLED.equals(open.get(0).getStatus())
                    ? "You are already registered for " + course.getCode()
                    : "You already asked to join " + course.getCode() + ". Please wait for the administration.");
        }

        List<String> missing = rules.missingPrerequisites(student.getId(), course);
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Missing prerequisite: " + String.join(", ", missing));
        }
        if (rules.seatsTaken(course) >= course.getCapacity()) {
            throw new ApiException(HttpStatus.CONFLICT, course.getCode() + " is full");
        }
        String clash = rules.clash(student.getId(), course);
        if (clash != null) throw new ApiException(HttpStatus.CONFLICT, clash);

        Enrollment e = new Enrollment();
        e.setStudentId(student.getId());
        e.setCourseCode(course.getCode());
        e.setTerm(term);
        e.setStatus(Enrollment.PENDING);
        enrollments.save(e);

        notifications.notifyAdmins("SYSTEM", "Course registration request",
                (student.getFirstName() + " " + student.getLastName()).trim() + " asks to join "
                        + course.getCode() + " · " + course.getName() + ".", null);
        return view(course, Enrollment.PENDING, "");
    }

    /** Cancels a waiting request, or drops an approved course. */
    public void drop(User student, String courseCode) {
        String term = courseService.term();
        String code = courseCode.trim().toUpperCase();
        List<Enrollment> open = enrollments.findByStudentIdAndCourseCodeAndTermAndStatusIn(
                student.getId(), code, term, List.of(Enrollment.ENROLLED, Enrollment.PENDING));
        if (open.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "You have no registration or request for " + code);
        }
        for (Enrollment e : open) {
            e.setStatus(Enrollment.DROPPED);
            e.setDecidedAt(Instant.now());
            enrollments.save(e);
        }
    }

    // ---------------------------------------------------------------- helpers

    public Set<String> enrolledCodes(String studentId) {
        return enrollments.findByStudentIdAndTermAndStatus(studentId, courseService.term(), Enrollment.ENROLLED)
                .stream().map(Enrollment::getCourseCode).collect(Collectors.toCollection(HashSet::new));
    }

    /** For each course, the most relevant registration: approved, then waiting, then declined, then dropped. */
    private Map<String, Enrollment> bestByCourse(String studentId, String term) {
        Map<String, Enrollment> best = new HashMap<>();
        for (Enrollment e : enrollments.findByStudentIdAndTerm(studentId, term)) {
            Enrollment cur = best.get(e.getCourseCode());
            if (cur == null || rank(e) > rank(cur)
                    || (rank(e) == rank(cur) && e.getEnrolledAt().isAfter(cur.getEnrolledAt()))) {
                best.put(e.getCourseCode(), e);
            }
        }
        return best;
    }

    private int rank(Enrollment e) {
        return switch (e.getStatus()) {
            case Enrollment.ENROLLED -> 3;
            case Enrollment.PENDING -> 2;
            case Enrollment.REJECTED -> 1;
            default -> 0;
        };
    }

    private List<String> schedule(String code) {
        return slots.findByCourseCode(code).stream()
                .sorted(Comparator.comparingInt((TimetableSlot s) -> TimetableService.DAYS.indexOf(s.getDay()))
                        .thenComparing(TimetableSlot::getStartTime))
                .map(s -> s.getDay() + " " + s.getStartTime() + "-" + s.getEndTime() + " · Room " + s.getRoom())
                .toList();
    }

    private CourseView view(CourseOffering c, String status, String note) {
        int taken = rules.seatsTaken(c);
        boolean hasSyllabus = (c.getSyllabus() != null && !c.getSyllabus().isBlank())
                || (c.getSyllabusUrl() != null && !c.getSyllabusUrl().isBlank());
        return new CourseView(c.getCode(), c.getName(), c.getDepartment(), c.getCreditHours(), c.getCapacity(),
                taken, Math.max(c.getCapacity() - taken, 0), c.getInstructorName(), c.getPrerequisites(),
                Enrollment.ENROLLED.equals(status), schedule(c.getCode()), status, note, hasSyllabus);
    }
}
