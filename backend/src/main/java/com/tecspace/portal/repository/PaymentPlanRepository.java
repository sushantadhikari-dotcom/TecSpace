package com.tecspace.portal.repository;

import com.tecspace.portal.model.PaymentPlan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PaymentPlanRepository extends MongoRepository<PaymentPlan, String> {
    Optional<PaymentPlan> findByStudentId(String studentId);
}
