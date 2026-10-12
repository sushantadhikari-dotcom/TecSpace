package com.tecspace.portal.service;

import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.LibraryCard;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.LibraryCardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;

@Service
public class LibraryCardService {

    private final LibraryCardRepository repository;
    private final SecureRandom random = new SecureRandom();

    public LibraryCardService(LibraryCardRepository repository) {
        this.repository = repository;
    }

    public LibraryCard apply(User student) {
        if (repository.findByStudentId(student.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a library card");
        }

        LocalDate today = LocalDate.now();
        String cardNumber;
        do {
            cardNumber = "LIB-" + today.getYear() + "-" + (10000 + random.nextInt(90000));
        } while (repository.existsByCardNumber(cardNumber));

        LibraryCard card = new LibraryCard();
        card.setStudentId(student.getId());
        card.setCardNumber(cardNumber);
        card.setIssueDate(today);
        card.setExpiryDate(today.plusYears(4));
        card.setQrCodePayload(cardNumber + "|" + student.getFirstName() + " " + student.getLastName()
                + "|" + student.getFaculty());
        card.setStatus("ACTIVE");
        return repository.save(card);
    }

    public LibraryCard get(String studentId) {
        LibraryCard card = repository.findByStudentId(studentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "You have not applied for a library card yet"));
        if (card.getExpiryDate().isBefore(LocalDate.now()) && "ACTIVE".equals(card.getStatus())) {
            card.setStatus("EXPIRED");
            repository.save(card);
        }
        return card;
    }
}
