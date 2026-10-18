package com.tecspace.portal.controller;

import com.tecspace.portal.dto.SyllabusView;
import com.tecspace.portal.service.EnrollmentService;
import com.tecspace.portal.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/courses")
public class StudentSyllabusController {

    private final EnrollmentService enrollmentService;
    private final UserService userService;

    public StudentSyllabusController(EnrollmentService enrollmentService, UserService userService) {
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

    /** Only for courses whose registration the administration approved. */
    @GetMapping("/{code}/syllabus")
    public SyllabusView syllabus(Authentication auth, @PathVariable String code) {
        return enrollmentService.syllabus(userService.getByEmail(auth.getName()), code);
    }
}
