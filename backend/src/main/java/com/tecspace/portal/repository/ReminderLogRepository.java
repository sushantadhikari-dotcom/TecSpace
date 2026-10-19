package com.tecspace.portal.repository;

import com.tecspace.portal.model.ReminderLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReminderLogRepository extends MongoRepository<ReminderLog, String> {
}
