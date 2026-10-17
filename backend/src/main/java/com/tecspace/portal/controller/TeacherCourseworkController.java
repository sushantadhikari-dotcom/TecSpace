package com.tecspace.portal.controller;

import com.tecspace.portal.dto.*;
import com.tecspace.portal.model.User;
import com.tecspace.portal.service.CourseworkService;
import com.tecspace.portal.service.GradebookService;
import com.tecspace.portal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Assignments and the gradebook, for the teacher of the course. */
@RestController
@RequestMapping("/api/teacher")
public class TeacherCourseworkController {

    private final UserService userService;
    private final CourseworkService coursework;
    private final GradebookService gradebook;

    public TeacherCourseworkController(UserService userService, CourseworkService coursework, GradebookService gradebook) {
        this.userService = userService;
        this.coursework = coursework;
        this.gradebook = gradebook;
    }

    private User me(Authentication auth) {
        return userService.getByEmail(auth.getName());
    }

    @GetMapping("/assignments")
    public List<AssignmentView> assignments(Authentication auth) {
        return coursework.forTeacher(me(auth));
    }

    @PostMapping("/assignments")
    public ResponseEntity<AssignmentView> create(Authentication auth, @Valid @RequestBody AssignmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(coursework.create(me(auth), req));
    }

    @PutMapping("/assignments/{id}")
    public AssignmentView update(Authentication auth, @PathVariable String id, @Valid @RequestBody AssignmentRequest req) {
        return coursework.update(me(auth), id, req);
    }

    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable String id) {
        coursework.delete(me(auth), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/assignments/{id}/submissions")
    public List<SubmissionRow> submissions(Authentication auth, @PathVariable String id) {
        return coursework.submissions(me(auth), id);
    }

    @PutMapping("/assignments/{id}/grade")
    public SubmissionRow grade(Authentication auth, @PathVariable String id, @Valid @RequestBody GradeSubmissionRequest req) {
        return coursework.grade(me(auth), id, req);
    }

    @GetMapping("/gradebook")
    public GradebookView gradebook(Authentication auth, @RequestParam String courseCode,
                                   @RequestParam(required = false) String trimesterName) {
        return gradebook.view(me(auth), courseCode, trimesterName);
    }

    @PutMapping("/gradebook")
    public Map<String, Object> saveGradebook(Authentication auth, @Valid @RequestBody GradebookSave req) {
        return Map.of("saved", gradebook.save(me(auth), req));
    }
}
