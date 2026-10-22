package com.tecspace.portal.controller;

import com.tecspace.portal.dto.ChangePasswordRequest;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.UserRepository;
import com.tecspace.portal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Available to every logged-in user, even before a temporary password has been changed. */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserService userService;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AccountController(UserService userService, UserRepository users, PasswordEncoder encoder) {
        this.userService = userService;
        this.users = users;
        this.encoder = encoder;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        User u = userService.getByEmail(auth.getName());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("universityEmail", u.getUniversityEmail());
        m.put("firstName", u.getFirstName());
        m.put("lastName", u.getLastName());
        m.put("role", u.getRole());
        m.put("permissions", u.getPermissions());
        m.put("faculty", u.getFaculty());
        m.put("session", u.getSession());
        m.put("mustChangePassword", u.isMustChangePassword());
        return m;
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest req) {
        User u = userService.getByEmail(auth.getName());
        if (!encoder.matches(req.currentPassword(), u.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Your current password is not correct");
        }
        if (encoder.matches(req.newPassword(), u.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a password that is different from the current one");
        }
        u.setPassword(encoder.encode(req.newPassword()));
        u.setMustChangePassword(false);
        users.save(u);
        return ResponseEntity.noContent().build();
    }
}
