package com.tecspace.portal.repository;

import com.tecspace.portal.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    List<AuditLog> findTop300ByOrderByTimestampDesc();
}
