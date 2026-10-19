package com.tecspace.portal.service;

import com.tecspace.portal.dto.InstallmentView;
import com.tecspace.portal.model.*;
import com.tecspace.portal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Sends reminders by itself every morning: fees due, assignments due tomorrow, exams tomorrow. */
@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    private final UserRepository users;
    private final BalanceService balance;
    private final PaymentPlanService plans;
    private final InvoiceRepository invoices;
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final EnrollmentRepository enrollments;
    private final CourseOfferingRepository courses;
    private final ExamSlotRepository exams;
    private final NotificationService notifications;
    private final ReminderLogRepository sent;

    public ReminderService(UserRepository users, BalanceService balance, PaymentPlanService plans,
                           InvoiceRepository invoices, AssignmentRepository assignments,
                           SubmissionRepository submissions, EnrollmentRepository enrollments,
                           CourseOfferingRepository courses, ExamSlotRepository exams,
                           NotificationService notifications, ReminderLogRepository sent) {
        this.users = users;
        this.balance = balance;
        this.plans = plans;
        this.invoices = invoices;
        this.assignments = assignments;
        this.submissions = submissions;
        this.enrollments = enrollments;
        this.courses = courses;
        this.exams = exams;
        this.notifications = notifications;
        this.sent = sent;
    }

    @Scheduled(cron = "${tecspace.reminders.cron:0 0 8 * * *}")
    public void daily() {
        Map<String, Integer> result = run();
        log.info("Daily reminders sent: {}", result);
    }

    public Map<String, Integer> run() {
        Map<String, Integer> out = new HashMap<>();
        out.put("fees", feeReminders());
        out.put("assignments", assignmentReminders());
        out.put("exams", examReminders());
        return out;
    }

    // ---------------------------------------------------------------- fees

    private int feeReminders() {
        int count = 0;
        LocalDate today = LocalDate.now();

        for (User s : users.findByRole("ROLE_STUDENT")) {
            BigDecimal remaining;
            try {
                remaining = balance.record(s.getId()).getRemainingBalance();
            } catch (RuntimeException e) {
                continue;
            }
            if (remaining.signum() <= 0) continue;

            // installments
            for (InstallmentView i : plans.view(s.getId()).installments()) {
                if ("PAID".equals(i.status())) continue;
                if ("OVERDUE".equals(i.status())) {
                    if (once("FEE-OVERDUE|" + s.getId() + "|" + today)) {
                        notifications.notify(s.getId(), "SYSTEM", "Fee installment overdue",
                                "An installment was due on " + i.dueDate() + ". You still owe " + remaining.toPlainString() + ". Please pay as soon as you can.", null);
                        count++;
                    }
                } else if (!i.dueDate().isAfter(today.plusDays(3))) {
                    if (once("FEE-DUE|" + s.getId() + "|" + i.dueDate())) {
                        notifications.notify(s.getId(), "SYSTEM", "Fee installment due soon",
                                "An installment of " + i.amount().toPlainString() + " is due on " + i.dueDate() + ".", null);
                        count++;
                    }
                }
            }

            // invoices
            for (Invoice inv : invoices.findByStudentIdOrderByIssuedAtDesc(s.getId())) {
                if (!Invoice.ISSUED.equals(inv.getStatus()) || inv.getDueDate() == null) continue;
                if (!inv.getDueDate().isAfter(today.plusDays(3)) && once("INV|" + inv.getId() + "|" + today)) {
                    boolean late = inv.getDueDate().isBefore(today);
                    notifications.notify(s.getId(), "SYSTEM", late ? "Fee invoice overdue" : "Fee invoice due soon",
                            "Invoice " + inv.getInvoiceNumber() + " " + (late ? "was due on " : "is due on ") + inv.getDueDate()
                                    + ". Balance: " + remaining.toPlainString() + ".", null);
                    count++;
                }
            }
        }
        return count;
    }

    // ---------------------------------------------------------------- assignments

    private int assignmentReminders() {
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limit = now.plusHours(24);

        for (Assignment a : assignments.findAll()) {
            if (a.getDueAt().isBefore(now) || a.getDueAt().isAfter(limit)) continue;
            CourseOffering course = courses.findByCode(a.getCourseCode()).orElse(null);
            if (course == null) continue;

            Set<String> done = submissions.findByAssignmentId(a.getId()).stream()
                    .map(Submission::getStudentId).collect(Collectors.toSet());
            for (Enrollment e : enrollments.findByCourseCodeAndTermAndStatus(course.getCode(), course.getTerm(), Enrollment.ENROLLED)) {
                if (done.contains(e.getStudentId())) continue;
                if (once("ASG|" + a.getId() + "|" + e.getStudentId())) {
                    notifications.notify(e.getStudentId(), "SYSTEM", "Assignment due soon: " + a.getTitle(),
                            course.getCode() + " · due " + a.getDueAt().toLocalDate() + " at " + a.getDueAt().toLocalTime().withSecond(0).withNano(0) + ". You have not handed it in yet.", null);
                    count++;
                }
            }
        }
        return count;
    }

    // ---------------------------------------------------------------- exams

    private int examReminders() {
        int count = 0;
        for (ExamSlot x : exams.findByDate(LocalDate.now().plusDays(1))) {
            CourseOffering course = courses.findByCode(x.getCourseCode()).orElse(null);
            if (course == null) continue;
            for (Enrollment e : enrollments.findByCourseCodeAndTermAndStatus(course.getCode(), course.getTerm(), Enrollment.ENROLLED)) {
                if (once("EXAM|" + x.getId() + "|" + e.getStudentId())) {
                    notifications.notify(e.getStudentId(), "EXAM", "Exam tomorrow: " + course.getCode(),
                            course.getName() + " at " + x.getStartTime() + ", room " + x.getRoom() + ". Download your admit card.", null);
                    count++;
                }
            }
        }
        return count;
    }

    /** True the first time a key is seen, false if that reminder was already sent. */
    private boolean once(String key) {
        try {
            sent.save(new ReminderLog(key));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
