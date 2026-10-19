package com.tecspace.portal.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.Attachment;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/** Stores uploaded files in MongoDB (GridFS) after checking size, type and the real file contents. */
@Service
public class FileStorageService {

    public static final Set<String> DOCUMENTS = Set.of("pdf", "png", "jpg", "jpeg", "docx");
    public static final Set<String> IMAGES = Set.of("png", "jpg", "jpeg");
    public static final Set<String> PDF_OR_IMAGES = Set.of("pdf", "png", "jpg", "jpeg");

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> TYPES = Map.of(
            "pdf", "application/pdf", "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    public record Stored(String fileId, String name, String contentType, long size) {}

    private final GridFsTemplate gridFs;

    public FileStorageService(GridFsTemplate gridFs) {
        this.gridFs = gridFs;
    }

    public Stored store(MultipartFile file, Set<String> allowed) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a file first");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "That file is too big. The limit is 5 MB.");
        }

        String name = cleanName(file.getOriginalFilename());
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
        if (!allowed.contains(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This file type is not allowed. Allowed: " + String.join(", ", allowed));
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The file could not be read");
        }
        if (!looksLike(ext, bytes)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The file contents do not match its type (." + ext + ")");
        }

        String contentType = TYPES.get(ext);
        ObjectId id = gridFs.store(new ByteArrayInputStream(bytes), name, contentType);
        return new Stored(id.toHexString(), name, contentType, bytes.length);
    }

    public void delete(String fileId) {
        gridFs.delete(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
    }

    public ResponseEntity<byte[]> respond(Attachment a, boolean inline) {
        GridFSFile f = gridFs.findOne(new Query(Criteria.where("_id").is(new ObjectId(a.getFileId()))));
        if (f == null) throw new ApiException(HttpStatus.NOT_FOUND, "The file is no longer available");
        try {
            byte[] bytes = gridFs.getResource(f).getInputStream().readAllBytes();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(a.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, (inline ? "inline" : "attachment") + "; filename=\"" + a.getName() + "\"")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(bytes);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "The file could not be read");
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Keeps only safe characters so the name cannot break headers or paths. */
    private String cleanName(String original) {
        String n = original == null ? "file" : original;
        n = n.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1);
        n = n.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        return n.isEmpty() ? "file" : (n.length() > 80 ? n.substring(n.length() - 80) : n);
    }

    private boolean looksLike(String ext, byte[] b) {
        if (b.length < 8) return false;
        return switch (ext) {
            case "pdf" -> b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F';
            case "png" -> (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "jpg", "jpeg" -> (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case "docx" -> b[0] == 'P' && b[1] == 'K';
            default -> false;
        };
    }
}
