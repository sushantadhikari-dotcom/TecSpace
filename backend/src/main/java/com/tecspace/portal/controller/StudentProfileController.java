package com.tecspace.portal.controller;

import com.tecspace.portal.dto.*;
import com.tecspace.portal.model.User;
import com.tecspace.portal.service.CourseworkService;
import com.tecspace.portal.service.ProfileService;
import com.tecspace.portal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** A student's own profile, enrolment history and assignments. */
@RestController
@RequestMapping("/api/student")
public class StudentProfileController {

    private final UserService userService;
    private final ProfileService profiles;
    private final CourseworkService coursework;

    public StudentProfileController(UserService userService, ProfileService profiles, CourseworkService coursework) {
        this.userService = userService;
        this.profiles = profiles;
        this.coursework = coursework;
    }

    @GetMapping("/profile")
    public ProfileData profile(Authentication auth) {
        return profiles.get(userService.getByEmail(auth.getName()).getId());
    }

    @PutMapping("/profile")
    public ProfileData saveProfile(Authentication auth, @Valid @RequestBody ProfileData req) {
        User me = userService.getByEmail(auth.getName());
        return profiles.update(me.getId(), req, me.getUniversityEmail());
    }

    @GetMapping("/history")
    public List<HistoryRow> history(Authentication auth) {
        return profiles.history(userService.getByEmail(auth.getName()).getId());
    }

    @GetMapping("/assignments")
    public List<StudentAssignmentView> assignments(Authentication auth) {
        return coursework.forStudent(userService.getByEmail(auth.getName()));
    }

    @PostMapping("/assignments/{id}/submit")
    public ResponseEntity<StudentAssignmentView> submit(Authentication auth, @PathVariable String id,
                                                        @Valid @RequestBody SubmitRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(coursework.submit(userService.getByEmail(auth.getName()), id, req));
    }
}
