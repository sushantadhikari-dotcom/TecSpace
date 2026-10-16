package com.tecspace.portal.service;

import com.tecspace.portal.dto.FeeItemRequest;
import com.tecspace.portal.dto.FeeStructureRequest;
import com.tecspace.portal.dto.GenerateInvoicesRequest;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.*;
import com.tecspace.portal.repository.FeeStructureRepository;
import com.tecspace.portal.repository.InvoiceRepository;
import com.tecspace.portal.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class FeeService {

    private final FeeStructureRepository structures;
    private final InvoiceRepository invoices;
    private final UserRepository users;
    private final UserService userService;
    private final BalanceService balance;
    private final CourseOfferingService courseService;
    private final NotificationService notifications;

    public FeeService(FeeStructureRepository structures, InvoiceRepository invoices, UserRepository users,
                      UserService userService, BalanceService balance, CourseOfferingService courseService,
                      NotificationService notifications) {
        this.structures = structures;
        this.invoices = invoices;
        this.users = users;
        this.userService = userService;
        this.balance = balance;
        this.courseService = courseService;
        this.notifications = notifications;
    }

    // ---------------------------------------------------------------- fee structures

    public List<FeeStructure> structures() {
        return structures.findAll();
    }

    public FeeStructure createStructure(FeeStructureRequest r) {
        FeeStructure s = new FeeStructure();
        s.setName(r.name().trim());
        s.setFaculty(r.faculty());
        s.setTerm(courseService.term());
        List<FeeItem> items = new ArrayList<>();
        for (FeeItemRequest i : r.items()) {
            FeeItem fi = new FeeItem();
            fi.setCategory(i.category());
            fi.setLabel(i.label().trim());
            fi.setAmount(i.amount());
            fi.setOptional(i.optional());
            items.add(fi);
        }
        s.setItems(items);
        return structures.save(s);
    }

    public void deleteStructure(String id) {
        if (invoices.existsByStructureIdAndStatus(id, Invoice.ISSUED)) {
            throw new ApiException(HttpStatus.CONFLICT, "Invoices were already issued from this structure. Cancel them first.");
        }
        structures.deleteById(id);
    }

    // ---------------------------------------------------------------- invoices

    public Map<String, Integer> generate(GenerateInvoicesRequest r) {
        FeeStructure s = structures.findById(r.structureId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Fee structure not found"));

        List<User> targets;
        if (r.studentId() != null && !r.studentId().isBlank()) {
            User u = userService.getStudentById(r.studentId());
            if (!matches(s, u)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "This fee structure is for " + s.getFaculty() + " students");
            }
            targets = List.of(u);
        } else {
            targets = users.findByRole("ROLE_STUDENT").stream().filter(u -> matches(s, u)).toList();
        }

        Set<String> optional = r.includeOptional() == null ? Set.of() : new HashSet<>(r.includeOptional());
        int created = 0;
        int skipped = 0;

        for (User u : targets) {
            if (invoices.existsByStudentIdAndStructureIdAndStatus(u.getId(), s.getId(), Invoice.ISSUED)) {
                skipped++;
                continue;
            }
            List<InvoiceItem> items = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;
            for (FeeItem fi : s.getItems()) {
                if (fi.isOptional() && !optional.contains(fi.getLabel())) continue;
                items.add(new InvoiceItem(fi.getCategory(), fi.getLabel(), fi.getAmount()));
                subtotal = subtotal.add(fi.getAmount());
            }
            if (items.isEmpty()) {
                skipped++;
                continue;
            }

            Invoice inv = new Invoice();
            inv.setInvoiceNumber(nextNumber());
            inv.setStudentId(u.getId());
            inv.setStructureId(s.getId());
            inv.setStructureName(s.getName());
            inv.setTerm(s.getTerm());
            inv.setItems(items);
            inv.setSubtotal(subtotal);
            inv.setDueDate(r.dueDate());
            invoices.save(inv);

            balance.adjustTotal(u.getId(), subtotal);
            notifications.notify(u.getId(), "SYSTEM", "New fee invoice " + inv.getInvoiceNumber(),
                    s.getName() + ": " + subtotal.toPlainString() + " due by " + r.dueDate() + ".", null);
            created++;
        }
        return Map.of("created", created, "skipped", skipped);
    }

    public Invoice cancel(String id) {
        Invoice inv = invoices.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (!Invoice.ISSUED.equals(inv.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This invoice is already cancelled");
        }
        FinancialRecord rec = balance.record(inv.getStudentId());
        if (rec.getTotalFee().subtract(inv.getSubtotal()).compareTo(rec.getPaidFee()) < 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "The student has already paid more than the fees would be after cancelling. Adjust the payments first.");
        }
        inv.setStatus(Invoice.CANCELLED);
        invoices.save(inv);
        balance.adjustTotal(inv.getStudentId(), inv.getSubtotal().negate());
        return inv;
    }

    public List<Invoice> all() {
        return invoices.findAllByOrderByIssuedAtDesc();
    }

    public List<Invoice> forStudent(String studentId) {
        return invoices.findByStudentIdOrderByIssuedAtDesc(studentId);
    }

    // ---------------------------------------------------------------- helpers

    private boolean matches(FeeStructure s, User u) {
        return "ALL".equals(s.getFaculty()) || s.getFaculty().equals(u.getFaculty());
    }

    private String nextNumber() {
        return "INV-" + LocalDate.now().getYear() + "-" + String.format("%05d", invoices.count() + 1);
    }
}
