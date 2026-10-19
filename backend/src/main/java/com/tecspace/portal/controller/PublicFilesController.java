package com.tecspace.portal.controller;

import com.tecspace.portal.dto.AttachmentView;
import com.tecspace.portal.service.AttachmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Applicants upload their documents without logging in. They must give their tracking code AND email. */
@RestController
@RequestMapping("/api/public/applications")
public class PublicFilesController {

    private final AttachmentService files;

    public PublicFilesController(AttachmentService files) {
        this.files = files;
    }

    @PostMapping(value = "/{code}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentView> upload(@PathVariable String code, @RequestParam String email,
                                                 @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(files.addApplicationFile(code, email, file));
    }

    @GetMapping("/{code}/files")
    public List<AttachmentView> list(@PathVariable String code, @RequestParam String email) {
        return files.applicationFilesPublic(code, email);
    }
}
