package com.tecspace.portal.repository;

import com.tecspace.portal.model.ResultTermState;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ResultTermStateRepository extends MongoRepository<ResultTermState, String> {
    Optional<ResultTermState> findByTrimesterName(String trimesterName);
    List<ResultTermState> findByLockedTrue();
    List<ResultTermState> findByPublishedTrue();
}
