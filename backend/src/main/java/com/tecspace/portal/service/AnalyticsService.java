package com.tecspace.portal.service;

import com.tecspace.portal.dto.AnalyticsView;
import com.tecspace.portal.dto.AttendanceStat;
import com.tecspace.portal.dto.Bar;
import com.tecspace.portal.dto.FinanceRow;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.*;
import com.tecspace.portal.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Numbers for the analytics dashboard and the CSV exports. */
@Service
public class AnalyticsService {

    private final UserRepository users;
    private final ApplicationRepository applications;
    private final CourseOfferingService courseService;
    private final CourseOfferingRepository courses;
    private final EnrollmentRepository enrollments;
    private final AttendanceService attendance;
    private final GradeRepository grades;
    private final GradingScaleService scales;
    private final FinancialRepository financials;
    private final LedgerService ledger;

    public AnalyticsService(UserRepository users, ApplicationRepository applications,
                            CourseOfferingService courseService, CourseOfferingRepository courses,
                            EnrollmentRepository enrollments, AttendanceService attendance, GradeRepository grades,
                            GradingScaleService scales, FinancialRepository financials, LedgerService ledger) {
        this.users = users;
        this.applications = applications;
        this.courseService = courseService;
        this.courses = courses;
        this.enrollments = enrollments;
        this.attendance = attendance;
        this.grades = grades;
        this.scales = scales;
        this.financials = financials;
        this.ledger = ledger;
    }

    public AnalyticsView overview() {
        List<User> students = users.findByRole("ROLE_STUDENT");
        Map<String, User> byId = students.stream().collect(Collectors.toMap(User::getId, Function.identity()));

        // students by faculty / session
        List<Bar> byFaculty = countBars(students.stream().map(User::getFaculty).toList());
        List<Bar> bySession = countBars(students.stream().map(User::getSession).toList());

        // application funnel
        List<String> order = List.of("SUBMITTED", "APPROVED", "WAITLISTED", "ENROLLED", "REJECTED");
        Map<String, Long> appCounts = applications.findAll().stream()
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));
        List<Bar> funnel = order.stream().map(s -> new Bar(label(s), appCounts.getOrDefault(s, 0L), 0)).toList();

        // how full each course is
        String term = courseService.term();
        List<Bar> fill = courses.findByTerm(term).stream()
                .map(c -> new Bar(c.getCode(),
                        enrollments.countByCourseCodeAndTermAndStatus(c.getCode(), term, Enrollment.ENROLLED), c.getCapacity()))
                .toList();

        // attendance
        List<AttendanceStat> rows = attendance.report(false, null);
        Map<String, Double> avgAttendance = rows.stream().filter(r -> r.sessions() > 0)
                .collect(Collectors.groupingBy(AttendanceStat::courseCode, TreeMap::new,
                        Collectors.averagingDouble(AttendanceStat::percent)));
        List<Bar> attBars = avgAttendance.entrySet().stream()
                .map(e -> new Bar(e.getKey(), Math.round(e.getValue() * 10) / 10.0, 100)).toList();
        int flagged = attendance.report(true, null).size();

        // grades
        List<GradeRecord> allGrades = grades.findAll();
        Map<String, Long> letters = allGrades.stream().flatMap(g -> g.getTrimesters().stream())
                .flatMap(t -> t.getCourses().stream()).map(Course::getGrade)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<Bar> dist = new ArrayList<>();
        for (GradeBand b : scales.active().getBands()) {
            dist.add(new Bar(b.getLetter(), letters.getOrDefault(b.getLetter(), 0L), 0));
        }
        dist.sort(Comparator.comparingInt(b -> scaleRank(b.label())));

        Map<String, List<Double>> cgpaByFaculty = new TreeMap<>();
        for (GradeRecord g : allGrades) {
            User u = byId.get(g.getStudentId());
            if (u != null && !g.getTrimesters().isEmpty()) {
                cgpaByFaculty.computeIfAbsent(u.getFaculty(), k -> new ArrayList<>()).add(g.getCgpa());
            }
        }
        List<Bar> avgCgpa = cgpaByFaculty.entrySet().stream().map(e -> new Bar(e.getKey(),
                Math.round(e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) / 100.0, 4)).toList();

        // money
        List<FinancialRecord> fin = financials.findAll();
        BigDecimal billed = fin.stream().map(FinancialRecord::getTotalFee).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal collected = fin.stream().map(FinancialRecord::getPaidFee).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = fin.stream().map(FinancialRecord::getRemainingBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<YearMonth, BigDecimal> perMonth = new TreeMap<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) perMonth.put(now.minusMonths(i), BigDecimal.ZERO);
        for (FinancialRecord r : fin) {
            for (PaymentTransaction t : r.getPaymentHistory()) {
                if (t.getAmount().signum() <= 0) continue;
                YearMonth m = YearMonth.from(t.getTimestamp().atZone(ZoneId.systemDefault()));
                perMonth.computeIfPresent(m, (k, v) -> v.add(t.getAmount()));
            }
        }
        List<Bar> monthly = perMonth.entrySet().stream()
                .map(e -> new Bar(e.getKey().toString(), e.getValue().doubleValue(), 0)).toList();

        return new AnalyticsView(students.size(), byFaculty, bySession, funnel, fill, attBars, flagged,
                dist, avgCgpa, monthly, billed, collected, outstanding);
    }

    /** Rows for a compliance / reporting CSV. The first row is the header. */
    public List<List<String>> export(String kind) {
        List<User> students = users.findByRole("ROLE_STUDENT");
        Map<String, User> byId = students.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<List<String>> out = new ArrayList<>();

        switch (kind) {
            case "students" -> {
                out.add(List.of("Name", "Email", "Mobile", "Faculty", "Session", "Joined"));
                for (User u : students) out.add(List.of(name(u), u.getUniversityEmail(), u.getMobileNumber(),
                        u.getFaculty(), u.getSession(), u.getCreatedAt().toString()));
            }
            case "enrollments" -> {
                out.add(List.of("Student", "Email", "Course", "Term", "Status", "Enrolled at"));
                for (Enrollment e : enrollments.findAll()) {
                    User u = byId.get(e.getStudentId());
                    if (u == null) continue;
                    out.add(List.of(name(u), u.getUniversityEmail(), e.getCourseCode(), e.getTerm(), e.getStatus(),
                            e.getEnrolledAt().toString()));
                }
            }
            case "attendance" -> {
                out.add(List.of("Student", "Course", "Attended", "Sessions", "Percent", "Status"));
                for (AttendanceStat r : attendance.report(false, null)) {
                    out.add(List.of(r.studentName(), r.courseCode(), String.valueOf(r.attended()),
                            String.valueOf(r.sessions()), String.valueOf(r.percent()), r.level()));
                }
            }
            case "finance" -> {
                out.add(List.of("Student", "Faculty", "Total fee", "Paid", "Remaining"));
                for (FinanceRow r : ledger.summary().rows()) {
                    out.add(List.of(r.name(), r.faculty(), r.totalFee().toPlainString(), r.paidFee().toPlainString(),
                            r.remaining().toPlainString()));
                }
            }
            case "grades" -> {
                out.add(List.of("Student", "Trimester", "Course code", "Course", "Credits", "Marks", "Grade"));
                for (GradeRecord g : grades.findAll()) {
                    User u = byId.get(g.getStudentId());
                    if (u == null) continue;
                    for (Trimester t : g.getTrimesters()) {
                        for (Course c : t.getCourses()) {
                            out.add(List.of(name(u), t.getTrimesterName(), c.getCourseCode(), c.getCourseName(),
                                    String.valueOf(c.getCreditHours()), c.getPercentage() == null ? "" : String.valueOf(c.getPercentage()),
                                    c.getGrade()));
                        }
                    }
                }
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown export: " + kind);
        }
        return out;
    }

    // ---------------------------------------------------------------- helpers

    private List<Bar> countBars(List<String> values) {
        return values.stream().collect(Collectors.groupingBy(Function.identity(), TreeMap::new, Collectors.counting()))
                .entrySet().stream().map(e -> new Bar(e.getKey(), e.getValue(), 0)).toList();
    }

    private int scaleRank(String letter) {
        List<GradeBand> bands = scales.active().getBands();
        for (int i = 0; i < bands.size(); i++) if (bands.get(i).getLetter().equals(letter)) return i;
        return bands.size();
    }

    private String label(String status) {
        return status.charAt(0) + status.substring(1).toLowerCase();
    }

    private String name(User u) {
        return (u.getFirstName() + " " + u.getLastName()).trim();
    }
}
