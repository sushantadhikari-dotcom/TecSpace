package com.tecspace.portal.repository;

import com.tecspace.portal.model.Announcement;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AnnouncementRepository extends MongoRepository<Announcement, String> {
    List<Announcement> findTop20ByOrderByCreatedAtDesc();
    List<Announcement> findTop5ByOrderByCreatedAtDesc();
}
