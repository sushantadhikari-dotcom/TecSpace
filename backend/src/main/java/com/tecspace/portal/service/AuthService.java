package com.tecspace.portal.service;

import com.tecspace.portal.dto.AuthResponse;
import com.tecspace.portal.dto.LoginRequest;
import com.tecspace.portal.dto.SignupRequest;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.AuditLog;
import com.tecspace.portal.model.FinancialRecord;
import com.tecspace.portal.model.GradeRecord;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.AuditLogRepository;
import com.tecspace.portal.repository.FinancialRepository;
import com.tecspace.portal.repository.GradeRepository;
import com.tecspace.portal.repository.UserRepository;
import com.tecspace.portal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final FinancialRepository financialRepository;
    private final GradeRepository gradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogRepository auditLogs;

    @Value("${tecspace.default-fee}")
    private BigDecimal defaultFee;

    @Value("${tecspace.security.max-failed-logins:5}")
    private int maxFailedLogins;

    @Value("${tecspace.security.lock-minutes:15}")
    private long lockMinutes;

    public AuthService(UserRepository userRepository, FinancialRepository financialRepository,
                       GradeRepository gradeRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       AuditLogRepository auditLogs) {
        this.userRepository = userRepository;
        this.financialRepository = financialRepository;
        this.gradeRepository = gradeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditLogs = auditLogs;
    }

    public AuthResponse signup(SignupRequest req) {
        String email = req.universityEmail().trim().toLowerCase();
        if (userRepository.existsByUniversityEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = new User();
        user.setUniversityEmail(email);
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setMobileNumber(req.mobileNumber());
        user.setFaculty(req.faculty());
        user.setSession(req.session());
        user.setRole("ROLE_STUDENT"); // public signup can only ever create students
        user = userRepository.save(user);

        FinancialRecord fin = new FinancialRecord();
        fin.setStudentId(user.getId());
        fin.setTotalFee(defaultFee);
        fin.setPaidFee(BigDecimal.ZERO);
        fin.setRemainingBalance(defaultFee);
        financialRepository.save(fin);

        GradeRecord grades = new GradeRecord();
        grades.setStudentId(user.getId());
        gradeRepository.save(grades);

        return toResponse(user);
    }

    /** Used when the administration creates a student account with a temporary password. */
    public AuthResponse signupTemporary(SignupRequest req) {
        AuthResponse created = signup(req);
        userRepository.findById(created.id()).ifPresent(u -> {
            u.setMustChangePassword(true);
            userRepository.save(u);
        });
        return new AuthResponse(created.token(), created.id(), created.universityEmail(), created.firstName(),
                created.lastName(), created.faculty(), created.session(), created.role(), true);
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.universityEmail().trim().toLowerCase();
        User user = userRepository.findByUniversityEmail(email).orElse(null);
        if (user == null) {
            audit(email, "LOGIN_FAILED", 401);
            throw new BadCredentialsException("bad credentials");
        }

        Instant now = Instant.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            long minutes = Math.max(1, Duration.between(now, user.getLockedUntil()).toMinutes() + 1);
            audit(email, "LOGIN_BLOCKED", 423);
            throw new ApiException(HttpStatus.LOCKED,
                    "Too many wrong passwords. This account is locked. Try again in " + minutes + " minute(s) or reset your password.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            user.setFailedLogins(user.getFailedLogins() + 1);
            if (user.getFailedLogins() >= maxFailedLogins) {
                user.setLockedUntil(now.plus(Duration.ofMinutes(lockMinutes)));
                user.setFailedLogins(0);
                audit(email, "ACCOUNT_LOCKED", 423);
            }
            userRepository.save(user);
            audit(email, "LOGIN_FAILED", 401);
            throw new BadCredentialsException("bad credentials");
        }

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is disabled. Please contact the administration.");
        }

        if (user.getFailedLogins() != 0 || user.getLockedUntil() != null) {
            user.setFailedLogins(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User u) {
        String token = jwtUtil.generateToken(u.getUniversityEmail(), u.getRole());
        return new AuthResponse(token, u.getId(), u.getUniversityEmail(), u.getFirstName(), u.getLastName(),
                u.getFaculty(), u.getSession(), u.getRole(), u.isMustChangePassword());
    }

    private void audit(String actor, String event, int status) {
        try {
            AuditLog log = new AuditLog();
            log.setActor(actor);
            log.setRole("-");
            log.setMethod(event);
            log.setPath("/api/auth/login");
            log.setStatus(status);
            log.setIp(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes s
                    ? s.getRequest().getRemoteAddr() : "-");
            auditLogs.save(log);
        } catch (Exception ignored) {
            // logging must never break a login
        }
    }
}
