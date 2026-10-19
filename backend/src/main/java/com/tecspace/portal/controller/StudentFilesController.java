package com.tecspace.portal.controller;

import com.tecspace.portal.dto.AttachmentView;
import com.tecspace.portal.service.AttachmentService;
import com.tecspace.portal.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** A student's own uploads: files for an assignment, and the profile photo. */
@RestController
@RequestMapping("/api/student")
public class StudentFilesController {

    private final AttachmentService files;
    private final UserService userService;

    public StudentFilesController(AttachmentService files, UserService userService) {
        this.files = files;
        this.userService = userService;
    }

    @PostMapping(value = "/assignments/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentView> upload(Authentication auth, @PathVariable String id,
                                                 @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(files.addAssignmentFile(userService.getByEmail(auth.getName()), id, file));
    }

    @GetMapping("/assignments/{id}/files")
    public List<AttachmentView> list(Authentication auth, @PathVariable String id) {
        return files.assignmentFilesForStudent(userService.getByEmail(auth.getName()), id);
    }

    @DeleteMapping("/files/{attachmentId}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable String attachmentId) {
        files.deleteStudentFile(userService.getByEmail(auth.getName()), attachmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/files/{attachmentId}/download")
    public ResponseEntity<byte[]> download(Authentication auth, @PathVariable String attachmentId) {
        return files.downloadForStudent(userService.getByEmail(auth.getName()), attachmentId);
    }

    @PutMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> setPhoto(Authentication auth, @RequestParam("file") MultipartFile file) {
        files.setPhoto(userService.getByEmail(auth.getName()), file);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/photo")
    public ResponseEntity<byte[]> photo(Authentication auth) {
        return files.photo(userService.getByEmail(auth.getName()).getId());
    }
}
