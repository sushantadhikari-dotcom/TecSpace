package com.tecspace.portal.service;

import com.tecspace.portal.dto.AuthResponse;
import com.tecspace.portal.dto.LoginRequest;
import com.tecspace.portal.dto.SignupRequest;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.FinancialRecord;
import com.tecspace.portal.model.GradeRecord;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.FinancialRepository;
import com.tecspace.portal.repository.GradeRepository;
import com.tecspace.portal.repository.UserRepository;
import com.tecspace.portal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final FinancialRepository financialRepository;
    private final GradeRepository gradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${tecspace.default-fee}")
    private BigDecimal defaultFee;

    public AuthService(UserRepository userRepository, FinancialRepository financialRepository,
                       GradeRepository gradeRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.financialRepository = financialRepository;
        this.gradeRepository = gradeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByUniversityEmail(req.universityEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("bad credentials");
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User u) {
        String token = jwtUtil.generateToken(u.getUniversityEmail(), u.getRole());
        return new AuthResponse(token, u.getId(), u.getUniversityEmail(), u.getFirstName(), u.getLastName(),
                u.getFaculty(), u.getSession(), u.getRole());
    }
}
