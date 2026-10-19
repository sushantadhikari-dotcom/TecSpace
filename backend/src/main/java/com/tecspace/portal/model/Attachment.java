package com.tecspace.portal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A file stored in MongoDB (GridFS) and who it belongs to. */
@Data
@Document(collection = "attachments")
@CompoundIndex(name = "owner_idx", def = "{'ownerType': 1, 'ownerId': 1}")
public class Attachment {
    public static final String ASSIGNMENT_WORK = "ASSIGNMENT_WORK";   // ownerId = assignment id, studentId = who handed it in
    public static final String APPLICATION = "APPLICATION";           // ownerId = application id
    public static final String PROFILE_PHOTO = "PROFILE_PHOTO";       // ownerId = student id

    @Id
    private String id;
    private String ownerType;
    private String ownerId;
    private String studentId;
    private String fileId;        // id of the GridFS file
    private String name;
    private String contentType;
    private long size;
    private Instant uploadedAt = Instant.now();
}
