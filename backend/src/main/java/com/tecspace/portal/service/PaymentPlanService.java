package com.tecspace.portal.service;

import com.tecspace.portal.dto.InstallmentView;
import com.tecspace.portal.dto.PlanRequest;
import com.tecspace.portal.dto.PlanView;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.FinancialRecord;
import com.tecspace.portal.model.Installment;
import com.tecspace.portal.model.PaymentPlan;
import com.tecspace.portal.repository.PaymentPlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentPlanService {

    private final PaymentPlanRepository plans;
    private final BalanceService balance;
    private final UserService userService;
    private final NotificationService notifications;

    public PaymentPlanService(PaymentPlanRepository plans, BalanceService balance, UserService userService,
                              NotificationService notifications) {
        this.plans = plans;
        this.balance = balance;
        this.userService = userService;
        this.notifications = notifications;
    }

    /** Splits the student's CURRENT remaining balance into equal installments. A new plan replaces the old one. */
    public PlanView create(PlanRequest r) {
        userService.getStudentById(r.studentId());
        FinancialRecord rec = balance.record(r.studentId());
        BigDecimal remaining = rec.getRemainingBalance();
        if (remaining.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This student has nothing left to pay");
        }

        BigDecimal each = remaining.divide(BigDecimal.valueOf(r.count()), 2, RoundingMode.DOWN);
        List<Installment> list = new ArrayList<>();
        BigDecimal used = BigDecimal.ZERO;
        for (int i = 0; i < r.count(); i++) {
            BigDecimal amount = (i == r.count() - 1) ? remaining.subtract(used) : each;
            used = used.add(amount);
            list.add(new Installment(r.firstDueDate().plusDays((long) i * r.intervalDays()), amount));
        }

        PaymentPlan plan = plans.findByStudentId(r.studentId()).orElseGet(PaymentPlan::new);
        plan.setStudentId(r.studentId());
        plan.setPaidAtCreation(rec.getPaidFee());
        plan.setInstallments(list);
        plan.setCreatedAt(java.time.Instant.now());
        plans.save(plan);

        notifications.notify(r.studentId(), "SYSTEM", "Payment plan created",
                "Your fees are split into " + r.count() + " installments. The first is due on " + r.firstDueDate() + ".", null);
        return view(r.studentId());
    }

    public void delete(String studentId) {
        plans.findByStudentId(studentId).ifPresent(plans::delete);
    }

    /** Money paid since the plan started is applied to the earliest installments first. */
    public PlanView view(String studentId) {
        PaymentPlan plan = plans.findByStudentId(studentId).orElse(null);
        if (plan == null) return new PlanView(false, BigDecimal.ZERO, BigDecimal.ZERO, List.of());

        FinancialRecord rec = balance.record(studentId);
        BigDecimal left = rec.getPaidFee().subtract(plan.getPaidAtCreation()).max(BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        List<InstallmentView> out = new ArrayList<>();

        for (Installment i : plan.getInstallments()) {
            BigDecimal paid = left.min(i.getAmount());
            left = left.subtract(paid);
            String status;
            if (paid.compareTo(i.getAmount()) >= 0) status = "PAID";
            else if (i.getDueDate().isBefore(LocalDate.now())) status = "OVERDUE";
            else if (paid.signum() > 0) status = "PARTIAL";
            else status = "UPCOMING";
            out.add(new InstallmentView(i.getDueDate(), i.getAmount(), paid, status));
            total = total.add(i.getAmount());
            totalPaid = totalPaid.add(paid);
        }
        return new PlanView(true, total, totalPaid, out);
    }
}
