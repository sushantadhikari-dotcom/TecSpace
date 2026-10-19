package com.tecspace.portal.controller;

import com.tecspace.portal.dto.AttachmentView;
import com.tecspace.portal.service.AttachmentService;
import com.tecspace.portal.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** A teacher can open the files students handed in for their own courses. */
@RestController
@RequestMapping("/api/teacher")
public class TeacherFilesController {

    private final AttachmentService files;
    private final UserService userService;

    public TeacherFilesController(AttachmentService files, UserService userService) {
        this.files = files;
        this.userService = userService;
    }

    @GetMapping("/assignments/{id}/files")
    public List<AttachmentView> list(Authentication auth, @PathVariable String id) {
        return files.assignmentFilesForTeacher(userService.getByEmail(auth.getName()), id);
    }

    @GetMapping("/files/{attachmentId}/download")
    public ResponseEntity<byte[]> download(Authentication auth, @PathVariable String attachmentId) {
        return files.downloadForTeacher(userService.getByEmail(auth.getName()), attachmentId);
    }
}
