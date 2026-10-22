package com.tecspace.portal.controller;

import com.tecspace.portal.dto.*;
import com.tecspace.portal.model.AuditLog;
import com.tecspace.portal.repository.AuditLogRepository;
import com.tecspace.portal.service.FacultyService;
import com.tecspace.portal.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Administrator-only: staff and teacher accounts, permissions, teacher assignments and the audit log. */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final UserAdminService userAdmin;
    private final FacultyService faculty;
    private final AuditLogRepository auditLogs;

    public AdminUserController(UserAdminService userAdmin, FacultyService faculty, AuditLogRepository auditLogs) {
        this.userAdmin = userAdmin;
        this.faculty = faculty;
        this.auditLogs = auditLogs;
    }

    @GetMapping("/users")
    public List<UserView> users() {
        return userAdmin.list();
    }

    @GetMapping("/users/permissions")
    public Map<String, String> permissions() {
        return userAdmin.permissionCatalog();
    }

    @PostMapping("/users")
    public ResponseEntity<CreatedUser> create(@Valid @RequestBody UserCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAdmin.create(req));
    }

    @PutMapping("/users/{id}")
    public UserView update(Authentication auth, @PathVariable String id, @Valid @RequestBody UserUpdateRequest req) {
        return userAdmin.update(auth.getName(), id, req);
    }

    @PostMapping("/users/{id}/reset-password")
    public Map<String, String> resetPassword(@PathVariable String id) {
        return Map.of("temporaryPassword", userAdmin.resetPassword(id));
    }

    @PostMapping("/users/{id}/unlock")
    public Map<String, String> unlock(@PathVariable String id) {
        userAdmin.unlock(id);
        return Map.of("message", "Account unlocked");
    }

    @GetMapping("/faculty-assignments")
    public List<FacultyAssignmentView> assignments() {
        return faculty.all();
    }

    @PutMapping("/faculty-assignments/{teacherId}")
    public List<FacultyAssignmentView> assign(@PathVariable String teacherId, @Valid @RequestBody AssignRequest req) {
        faculty.assign(teacherId, req.courseCodes());
        return faculty.all();
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> audit(@RequestParam(required = false) String q) {
        List<AuditLog> rows = auditLogs.findTop300ByOrderByTimestampDesc();
        if (q == null || q.isBlank()) return rows;
        String needle = q.trim().toLowerCase();
        return rows.stream().filter(l -> l.getActor().toLowerCase().contains(needle)
                || l.getPath().toLowerCase().contains(needle) || l.getMethod().toLowerCase().contains(needle)).toList();
    }
}
