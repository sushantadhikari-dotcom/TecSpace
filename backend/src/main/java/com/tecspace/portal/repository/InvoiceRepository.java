package com.tecspace.portal.repository;

import com.tecspace.portal.model.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    List<Invoice> findByStudentIdOrderByIssuedAtDesc(String studentId);
    List<Invoice> findAllByOrderByIssuedAtDesc();
    boolean existsByStudentIdAndStructureIdAndStatus(String studentId, String structureId, String status);
    boolean existsByStructureIdAndStatus(String structureId, String status);
}
