package com.tecspace.portal.repository;

import com.tecspace.portal.model.LibraryCard;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LibraryCardRepository extends MongoRepository<LibraryCard, String> {
    Optional<LibraryCard> findByStudentId(String studentId);
    boolean existsByCardNumber(String cardNumber);
}
