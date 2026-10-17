package com.tecspace.portal.controller;

import com.tecspace.portal.dto.ProfileData;
import com.tecspace.portal.dto.StudentRecordView;
import com.tecspace.portal.dto.StudentSummary;
import com.tecspace.portal.model.AuditLog;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.AuditLogRepository;
import com.tecspace.portal.service.ProfileService;
import com.tecspace.portal.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Student records for staff with the MANAGE_STUDENTS permission. Every view is written to the audit log. */
@RestController
@RequestMapping("/api/admin/student-profiles")
public class AdminProfileController {

    private final UserService userService;
    private final ProfileService profiles;
    private final AuditLogRepository auditLogs;

    public AdminProfileController(UserService userService, ProfileService profiles, AuditLogRepository auditLogs) {
        this.userService = userService;
        this.profiles = profiles;
        this.auditLogs = auditLogs;
    }

    @GetMapping("/{studentId}")
    public StudentRecordView view(Authentication auth, HttpServletRequest request, @PathVariable String studentId) {
        User student = userService.getStudentById(studentId);

        // Reading medical records is sensitive, so reads are logged too (the audit filter only logs changes)
        AuditLog log = new AuditLog();
        log.setActor(auth.getName());
        log.setRole(auth.getAuthorities().isEmpty() ? "-" : auth.getAuthorities().iterator().next().getAuthority());
        log.setMethod("VIEW");
        log.setPath("/api/admin/student-profiles/" + studentId);
        log.setStatus(200);
        log.setIp(request.getRemoteAddr());
        auditLogs.save(log);

        return new StudentRecordView(StudentSummary.from(student), profiles.get(studentId), profiles.history(studentId));
    }

    @PutMapping("/{studentId}")
    public ProfileData save(Authentication auth, @PathVariable String studentId, @Valid @RequestBody ProfileData req) {
        userService.getStudentById(studentId);
        return profiles.update(studentId, req, auth.getName());
    }
}
