package com.tecspace.portal.service;

import com.tecspace.portal.dto.*;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Exam admit cards. A student may sit an exam only if the fees are cleared and attendance in that course
 * is not below the minimum. The QR code on the card carries a signed token so invigilators can check it.
 */
@Service
public class AdmitCardService {

    private final ExamService examService;
    private final AttendanceService attendance;
    private final BalanceService balance;
    private final UserRepository users;

    @Value("${tecspace.jwt.secret}")
    private String secret;

    @Value("${tecspace.admit-card.require-fees-paid:true}")
    private boolean requireFees;

    public AdmitCardService(ExamService examService, AttendanceService attendance, BalanceService balance,
                            UserRepository users) {
        this.examService = examService;
        this.attendance = attendance;
        this.balance = balance;
        this.users = users;
    }

    public AdmitCardView forStudent(User student) {
        BigDecimal remaining = balance.record(student.getId()).getRemainingBalance();
        boolean feeCleared = remaining.signum() <= 0;
        double minimum = attendance.threshold();

        Map<String, AttendanceStat> byCourse = attendance.forStudent(student).stream()
                .collect(Collectors.toMap(AttendanceStat::courseCode, Function.identity(), (a, b) -> a));

        LocalDate today = LocalDate.now();
        List<AdmitExam> exams = new ArrayList<>();
        for (ExamEntry e : examService.forStudent(student)) {
            if (e.date().isBefore(today)) continue;

            List<String> reasons = new ArrayList<>();
            if (requireFees && !feeCleared) reasons.add("Fees are not fully paid");
            AttendanceStat stat = byCourse.get(e.courseCode());
            if (stat != null && "LOW".equals(stat.level())) {
                reasons.add("Attendance is below " + (int) minimum + "%");
            }
            exams.add(new AdmitExam(e.id(), e.courseCode(), e.courseName(), e.date(), e.startTime(), e.endTime(),
                    e.room(), stat == null || stat.sessions() == 0 ? null : stat.percent(), reasons.isEmpty(), reasons));
        }

        return new AdmitCardView(student.getId(), (student.getFirstName() + " " + student.getLastName()).trim(),
                student.getUniversityEmail(), student.getFaculty(), student.getSession(), today,
                feeCleared, remaining, minimum, exams, token(student.getId()));
    }

    /** Used by the page the QR code opens. Anyone with the code can see the result, nothing more. */
    public VerifyView verify(String studentId, String token) {
        if (token == null || !constantTimeEquals(token(studentId), token)) {
            return new VerifyView(false, "", "", "", List.of());
        }
        User student = users.findById(studentId).orElse(null);
        if (student == null || !"ROLE_STUDENT".equals(student.getRole())) {
            return new VerifyView(false, "", "", "", List.of());
        }
        AdmitCardView card = forStudent(student);
        return new VerifyView(true, card.studentName(), card.faculty(), card.session(), card.exams());
    }

    public String token(String studentId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(("ADMIT|" + studentId).getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign the admit card", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
