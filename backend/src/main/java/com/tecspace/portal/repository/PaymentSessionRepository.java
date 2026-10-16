package com.tecspace.portal.repository;

import com.tecspace.portal.model.PaymentSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentSessionRepository extends MongoRepository<PaymentSession, String> {
    Optional<PaymentSession> findByToken(String token);
    Optional<PaymentSession> findByPidx(String pidx);
    List<PaymentSession> findByStudentIdAndStatusInOrderByCreatedAtDesc(String studentId, Collection<String> statuses);
}
