package com.tecspace.portal.service;

import com.tecspace.portal.dto.AttendanceEntryRequest;
import com.tecspace.portal.dto.AttendanceSaveRequest;
import com.tecspace.portal.dto.AttendanceStat;
import com.tecspace.portal.dto.RosterRow;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.*;
import com.tecspace.portal.repository.AttendanceRepository;
import com.tecspace.portal.repository.CourseOfferingRepository;
import com.tecspace.portal.repository.EnrollmentRepository;
import com.tecspace.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Attendance rules:
 *  - PRESENT and LATE count as attended, ABSENT counts against the student.
 *  - EXCUSED sessions are ignored (they are not counted at all).
 *  - percentage = attended / counted sessions.
 *  - LOW = below the threshold, WARNING = within 5 points above it.
 *  - "trending" = 3 or more absences in the last 5 counted sessions.
 */
@Service
public class AttendanceService {

    public static final List<String> STATUSES = List.of("PRESENT", "ABSENT", "LATE", "EXCUSED");

    private final AttendanceRepository attendance;
    private final EnrollmentRepository enrollments;
    private final CourseOfferingService courseService;
    private final CourseOfferingRepository courses;
    private final UserRepository users;
    private final NotificationService notifications;

    @Value("${tecspace.attendance.threshold:75}")
    private double threshold;

    public AttendanceService(AttendanceRepository attendance, EnrollmentRepository enrollments,
                             CourseOfferingService courseService, CourseOfferingRepository courses,
                             UserRepository users, NotificationService notifications) {
        this.attendance = attendance;
        this.enrollments = enrollments;
        this.courseService = courseService;
        this.courses = courses;
        this.users = users;
        this.notifications = notifications;
    }

    public double threshold() {
        return threshold;
    }

    // ---------------------------------------------------------------- admin: daily register

    /** The automatic register: every enrolled student, with the status already taken for that day (if any). */
    public List<RosterRow> roster(String courseCode, LocalDate date) {
        CourseOffering course = courseService.get(courseCode);
        Map<String, String> existing = attendance.findByCourseCodeAndDate(course.getCode(), date)
                .map(r -> r.getEntries().stream()
                        .collect(Collectors.toMap(AttendanceEntry::getStudentId, AttendanceEntry::getStatus, (a, b) -> b)))
                .orElse(Map.of());
        return enrolledStudents(course).stream()
                .map(u -> new RosterRow(u.getId(), fullName(u), existing.get(u.getId())))
                .toList();
    }

    public int save(AttendanceSaveRequest req) {
        CourseOffering course = courseService.get(req.courseCode());
        if (req.date().isAfter(LocalDate.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot take attendance for a future date");
        }

        List<User> students = enrolledStudents(course);
        Set<String> allowed = students.stream().map(User::getId).collect(Collectors.toSet());

        Map<String, String> chosen = new LinkedHashMap<>();
        for (AttendanceEntryRequest e : req.entries()) {
            String status = e.status().trim().toUpperCase();
            if (!STATUSES.contains(status)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Status must be PRESENT, ABSENT, LATE or EXCUSED");
            }
            if (!allowed.contains(e.studentId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A student on the list is not enrolled in " + course.getCode());
            }
            chosen.put(e.studentId(), status);
        }

        Map<String, AttendanceStat> before = statsByStudent(course, students, threshold);

        AttendanceRecord rec = attendance.findByCourseCodeAndDate(course.getCode(), req.date())
                .orElseGet(AttendanceRecord::new);
        rec.setCourseCode(course.getCode());
        rec.setTerm(course.getTerm());
        rec.setDate(req.date());
        rec.setEntries(chosen.entrySet().stream()
                .map(e -> new AttendanceEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(ArrayList::new)));
        attendance.save(rec);

        // Warn students whose attendance just got worse and is now at WARNING or LOW
        Map<String, AttendanceStat> after = statsByStudent(course, students, threshold);
        for (User u : students) {
            AttendanceStat b = before.get(u.getId());
            AttendanceStat a = after.get(u.getId());
            if (a != null && b != null && rank(a.level()) > rank(b.level())) {
                notifications.notify(u.getId(), "ATTENDANCE",
                        "Attendance " + (a.level().equals("LOW") ? "is low" : "warning") + ": " + course.getCode(),
                        "Your attendance in " + course.getCode() + " " + course.getName() + " is " + a.percent()
                                + "%. The minimum is " + (int) threshold + "%.",
                        null);
            }
        }
        return chosen.size();
    }

    // ---------------------------------------------------------------- reports

    /** Every enrolled student in every course this term, worst attendance first. */
    public List<AttendanceStat> report(boolean flaggedOnly, Double thresholdOverride) {
        double th = thresholdOverride == null ? threshold : thresholdOverride;
        List<AttendanceStat> rows = new ArrayList<>();
        for (CourseOffering c : courseService.list()) {
            rows.addAll(statsByStudent(c, enrolledStudents(c), th).values());
        }
        return rows.stream()
                .filter(r -> !flaggedOnly || r.level().equals("LOW") || r.level().equals("WARNING") || r.trending())
                .sorted(Comparator.comparingDouble(AttendanceStat::percent))
                .toList();
    }

    public List<AttendanceStat> forStudent(User student) {
        String term = courseService.term();
        List<AttendanceStat> out = new ArrayList<>();
        for (Enrollment e : enrollments.findByStudentIdAndTermAndStatus(student.getId(), term, Enrollment.ENROLLED)) {
            courses.findByCode(e.getCourseCode()).ifPresent(c -> {
                AttendanceStat s = statsByStudent(c, List.of(student), threshold).get(student.getId());
                if (s != null) out.add(s);
            });
        }
        return out;
    }

    // ---------------------------------------------------------------- helpers

    private Map<String, AttendanceStat> statsByStudent(CourseOffering course, List<User> students, double th) {
        List<AttendanceRecord> records = attendance.findByCourseCodeAndTerm(course.getCode(), course.getTerm()).stream()
                .sorted(Comparator.comparing(AttendanceRecord::getDate)).toList();
        Map<String, AttendanceStat> out = new LinkedHashMap<>();
        for (User u : students) out.put(u.getId(), compute(u, course, records, th));
        return out;
    }

    private AttendanceStat compute(User u, CourseOffering c, List<AttendanceRecord> records, double th) {
        int attended = 0;
        int sessions = 0;
        List<Boolean> counted = new ArrayList<>();   // true = attended, in date order

        for (AttendanceRecord r : records) {
            for (AttendanceEntry e : r.getEntries()) {
                if (!e.getStudentId().equals(u.getId())) continue;
                switch (e.getStatus()) {
                    case "PRESENT", "LATE" -> { attended++; sessions++; counted.add(true); }
                    case "ABSENT" -> { sessions++; counted.add(false); }
                    default -> { /* EXCUSED: not counted */ }
                }
                break;
            }
        }

        double pct = sessions == 0 ? 100.0 : Math.round(attended * 1000.0 / sessions) / 10.0;
        String level = sessions == 0 ? "NO_DATA" : pct < th ? "LOW" : pct < th + 5 ? "WARNING" : "OK";

        int recentAbsences = 0;
        for (int i = Math.max(0, counted.size() - 5); i < counted.size(); i++) {
            if (!counted.get(i)) recentAbsences++;
        }
        return new AttendanceStat(u.getId(), fullName(u), c.getCode(), c.getName(),
                attended, sessions, pct, level, recentAbsences, recentAbsences >= 3);
    }

    private int rank(String level) {
        return switch (level) {
            case "LOW" -> 2;
            case "WARNING" -> 1;
            default -> 0;
        };
    }

    private List<User> enrolledStudents(CourseOffering course) {
        List<String> ids = enrollments
                .findByCourseCodeAndTermAndStatus(course.getCode(), course.getTerm(), Enrollment.ENROLLED)
                .stream().map(Enrollment::getStudentId).toList();
        List<User> list = new ArrayList<>();
        users.findAllById(ids).forEach(list::add);
        list.sort(Comparator.comparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private String fullName(User u) {
        return (u.getFirstName() + " " + u.getLastName()).trim();
    }
}
