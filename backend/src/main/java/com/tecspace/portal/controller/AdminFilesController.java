package com.tecspace.portal.controller;

import com.tecspace.portal.dto.AttachmentView;
import com.tecspace.portal.service.AttachmentService;
import com.tecspace.portal.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Staff view application documents (MANAGE_ADMISSIONS) and student photos (MANAGE_STUDENTS). */
@RestController
@RequestMapping("/api/admin")
public class AdminFilesController {

    private final AttachmentService files;
    private final UserService userService;

    public AdminFilesController(AttachmentService files, UserService userService) {
        this.files = files;
        this.userService = userService;
    }

    @GetMapping("/applications/{id}/files")
    public List<AttachmentView> applicationFiles(@PathVariable String id) {
        return files.applicationFilesAdmin(id);
    }

    @GetMapping("/applications/files/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadApplicationFile(@PathVariable String attachmentId) {
        return files.downloadApplicationFile(attachmentId);
    }

    @GetMapping("/student-profiles/{studentId}/photo")
    public ResponseEntity<byte[]> studentPhoto(@PathVariable String studentId) {
        userService.getStudentById(studentId);
        return files.photo(studentId);
    }
}
