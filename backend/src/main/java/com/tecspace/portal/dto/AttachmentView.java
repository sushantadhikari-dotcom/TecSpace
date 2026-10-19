package com.tecspace.portal.dto;

import com.tecspace.portal.model.Attachment;

import java.time.Instant;

public record AttachmentView(String id, String studentId, String name, String contentType, long size, Instant uploadedAt) {
    public static AttachmentView from(Attachment a) {
        return new AttachmentView(a.getId(), a.getStudentId(), a.getName(), a.getContentType(), a.getSize(), a.getUploadedAt());
    }
}
