package com.tecspace.portal.repository;

import com.tecspace.portal.model.Attachment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends MongoRepository<Attachment, String> {
    List<Attachment> findByOwnerTypeAndOwnerId(String ownerType, String ownerId);
    List<Attachment> findByOwnerTypeAndOwnerIdAndStudentId(String ownerType, String ownerId, String studentId);
    Optional<Attachment> findFirstByOwnerTypeAndOwnerId(String ownerType, String ownerId);
}
