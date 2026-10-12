package com.tecspace.portal.repository;

import com.tecspace.portal.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUniversityEmail(String universityEmail);
    boolean existsByUniversityEmail(String universityEmail);
    List<User> findByRole(String role);
}
