package com.tecspace.portal.controller;

import com.tecspace.portal.dto.AdminFinancialRequest;
import com.tecspace.portal.dto.CashPaymentRequest;
import com.tecspace.portal.dto.FinancialResponse;
import com.tecspace.portal.dto.GradeUpdateRequest;
import com.tecspace.portal.dto.StudentSummary;
import com.tecspace.portal.model.GradeRecord;
import com.tecspace.portal.repository.UserRepository;
import com.tecspace.portal.service.GradeService;
import com.tecspace.portal.service.PaymentService;
import com.tecspace.portal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PaymentService paymentService;
    private final GradeService gradeService;

    public AdminController(UserRepository userRepository, UserService userService,
                           PaymentService paymentService, GradeService gradeService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.paymentService = paymentService;
        this.gradeService = gradeService;
    }

    @GetMapping("/students")
    public List<StudentSummary> students() {
        return userRepository.findByRole("ROLE_STUDENT").stream().map(StudentSummary::from).toList();
    }

    @GetMapping("/financials/{studentId}")
    public FinancialResponse getFinancials(@PathVariable String studentId) {
        userService.getStudentById(studentId);
        return paymentService.getFinancials(studentId);
    }

    @PutMapping("/financials/{studentId}")
    public FinancialResponse updateFinancials(@PathVariable String studentId,
                                              @Valid @RequestBody AdminFinancialRequest req) {
        userService.getStudentById(studentId);
        return paymentService.adminUpdate(studentId, req);
    }

    /** Admin records cash handed over by the student. */
    @PostMapping("/financials/{studentId}/cash")
    public FinancialResponse recordCash(@PathVariable String studentId,
                                        @Valid @RequestBody CashPaymentRequest req) {
        userService.getStudentById(studentId);
        return paymentService.recordCashPayment(studentId, req.amount());
    }

    @GetMapping("/grades/{studentId}")
    public GradeRecord getGrades(@PathVariable String studentId) {
        userService.getStudentById(studentId);
        return gradeService.get(studentId);
    }

    @PutMapping("/grades/{studentId}")
    public GradeRecord updateGrades(@PathVariable String studentId,
                                    @Valid @RequestBody GradeUpdateRequest req) {
        userService.getStudentById(studentId);
        return gradeService.update(studentId, req.trimesters());
    }
}
