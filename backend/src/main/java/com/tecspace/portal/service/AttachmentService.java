package com.tecspace.portal.service;

import com.tecspace.portal.dto.AttachmentView;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.*;
import com.tecspace.portal.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/** Who may upload, list, download and delete which files. */
@Service
public class AttachmentService {

    private final AttachmentRepository attachments;
    private final FileStorageService storage;
    private final AssignmentRepository assignments;
    private final SubmissionRepository submissions;
    private final EnrollmentRepository enrollments;
    private final ApplicationRepository applications;
    private final CourseOfferingService courseService;
    private final FacultyService faculty;

    public AttachmentService(AttachmentRepository attachments, FileStorageService storage,
                             AssignmentRepository assignments, SubmissionRepository submissions,
                             EnrollmentRepository enrollments, ApplicationRepository applications,
                             CourseOfferingService courseService, FacultyService faculty) {
        this.attachments = attachments;
        this.storage = storage;
        this.assignments = assignments;
        this.submissions = submissions;
        this.enrollments = enrollments;
        this.applications = applications;
        this.courseService = courseService;
        this.faculty = faculty;
    }

    // ================================================================ assignment work

    public AttachmentView addAssignmentFile(User student, String assignmentId, MultipartFile file) {
        Assignment a = assignment(assignmentId);
        CourseOffering course = courseService.get(a.getCourseCode());
        enrollments.findByStudentIdAndCourseCodeAndTermAndStatus(student.getId(), course.getCode(), course.getTerm(), Enrollment.ENROLLED)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not registered for " + course.getCode()));

        Submission sub = submissions.findByAssignmentIdAndStudentId(assignmentId, student.getId()).orElse(null);
        if (sub != null && sub.getMarks() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "This work was already marked, so it cannot be changed");
        }
        if (attachments.findByOwnerTypeAndOwnerIdAndStudentId(Attachment.ASSIGNMENT_WORK, assignmentId, student.getId()).size() >= 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can attach at most 5 files. Delete one first.");
        }

        FileStorageService.Stored s = storage.store(file, FileStorageService.DOCUMENTS);
        Attachment att = new Attachment();
        att.setOwnerType(Attachment.ASSIGNMENT_WORK);
        att.setOwnerId(assignmentId);
        att.setStudentId(student.getId());
        att.setFileId(s.fileId());
        att.setName(s.name());
        att.setContentType(s.contentType());
        att.setSize(s.size());
        att = attachments.save(att);

        // A file counts as handing in, so the teacher sees it even if no text was written
        if (sub == null) {
            sub = new Submission();
            sub.setAssignmentId(assignmentId);
            sub.setStudentId(student.getId());
        }
        sub.setSubmittedAt(Instant.now());
        sub.setLate(LocalDateTime.now().isAfter(a.getDueAt()));
        submissions.save(sub);
        return AttachmentView.from(att);
    }

    public List<AttachmentView> assignmentFilesForStudent(User student, String assignmentId) {
        return attachments.findByOwnerTypeAndOwnerIdAndStudentId(Attachment.ASSIGNMENT_WORK, assignmentId, student.getId())
                .stream().map(AttachmentView::from).toList();
    }

    public void deleteStudentFile(User student, String attachmentId) {
        Attachment att = mineOrFail(student, attachmentId);
        boolean marked = submissions.findByAssignmentIdAndStudentId(att.getOwnerId(), student.getId())
                .map(s -> s.getMarks() != null).orElse(false);
        if (marked) throw new ApiException(HttpStatus.CONFLICT, "This work was already marked, so it cannot be changed");
        storage.delete(att.getFileId());
        attachments.delete(att);
    }

    public ResponseEntity<byte[]> downloadForStudent(User student, String attachmentId) {
        return storage.respond(mineOrFail(student, attachmentId), false);
    }

    public List<AttachmentView> assignmentFilesForTeacher(User teacher, String assignmentId) {
        Assignment a = assignment(assignmentId);
        faculty.requireAssigned(teacher.getId(), a.getCourseCode());
        return attachments.findByOwnerTypeAndOwnerId(Attachment.ASSIGNMENT_WORK, assignmentId)
                .stream().map(AttachmentView::from).toList();
    }

    public ResponseEntity<byte[]> downloadForTeacher(User teacher, String attachmentId) {
        Attachment att = attachment(attachmentId);
        if (!Attachment.ASSIGNMENT_WORK.equals(att.getOwnerType())) throw notFound();
        faculty.requireAssigned(teacher.getId(), assignment(att.getOwnerId()).getCourseCode());
        return storage.respond(att, false);
    }

    // ================================================================ application documents

    public AttachmentView addApplicationFile(String code, String email, MultipartFile file) {
        Application app = application(code, email);
        if (Application.REJECTED.equals(app.getStatus()) || Application.ENROLLED.equals(app.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This application is closed, so files cannot be added");
        }
        if (attachments.findByOwnerTypeAndOwnerId(Attachment.APPLICATION, app.getId()).size() >= 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can upload at most 6 files");
        }
        FileStorageService.Stored s = storage.store(file, FileStorageService.PDF_OR_IMAGES);
        Attachment att = new Attachment();
        att.setOwnerType(Attachment.APPLICATION);
        att.setOwnerId(app.getId());
        att.setFileId(s.fileId());
        att.setName(s.name());
        att.setContentType(s.contentType());
        att.setSize(s.size());
        return AttachmentView.from(attachments.save(att));
    }

    public List<AttachmentView> applicationFilesPublic(String code, String email) {
        return attachments.findByOwnerTypeAndOwnerId(Attachment.APPLICATION, application(code, email).getId())
                .stream().map(AttachmentView::from).toList();
    }

    public List<AttachmentView> applicationFilesAdmin(String applicationId) {
        return attachments.findByOwnerTypeAndOwnerId(Attachment.APPLICATION, applicationId)
                .stream().map(AttachmentView::from).toList();
    }

    public ResponseEntity<byte[]> downloadApplicationFile(String attachmentId) {
        Attachment att = attachment(attachmentId);
        if (!Attachment.APPLICATION.equals(att.getOwnerType())) throw notFound();
        return storage.respond(att, false);
    }

    // ================================================================ profile photo

    public void setPhoto(User student, MultipartFile file) {
        FileStorageService.Stored s = storage.store(file, FileStorageService.IMAGES);
        attachments.findFirstByOwnerTypeAndOwnerId(Attachment.PROFILE_PHOTO, student.getId()).ifPresent(old -> {
            storage.delete(old.getFileId());
            attachments.delete(old);
        });
        Attachment att = new Attachment();
        att.setOwnerType(Attachment.PROFILE_PHOTO);
        att.setOwnerId(student.getId());
        att.setStudentId(student.getId());
        att.setFileId(s.fileId());
        att.setName(s.name());
        att.setContentType(s.contentType());
        att.setSize(s.size());
        attachments.save(att);
    }

    public ResponseEntity<byte[]> photo(String studentId) {
        Attachment att = attachments.findFirstByOwnerTypeAndOwnerId(Attachment.PROFILE_PHOTO, studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No photo yet"));
        return storage.respond(att, true);
    }

    // ================================================================ helpers

    private Attachment attachment(String id) {
        return attachments.findById(id).orElseThrow(this::notFound);
    }

    private Attachment mineOrFail(User student, String id) {
        Attachment a = attachment(id);
        if (!Attachment.ASSIGNMENT_WORK.equals(a.getOwnerType()) || !student.getId().equals(a.getStudentId())) throw notFound();
        return a;
    }

    private Assignment assignment(String id) {
        return assignments.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    private Application application(String code, String email) {
        return applications.findByTrackingCode(code.trim().toUpperCase())
                .filter(a -> a.getEmail().equalsIgnoreCase(email.trim()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No application found with that code and email"));
    }

    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "File not found");
    }
}
