package com.tecspace.portal.service;

import com.tecspace.portal.dto.AdminRequestView;
import com.tecspace.portal.dto.CertificateView;
import com.tecspace.portal.dto.RequestCreate;
import com.tecspace.portal.dto.RequestDecision;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.StudentRequest;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.StudentRequestRepository;
import com.tecspace.portal.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Self-service requests: transcripts, ID re-issue, bonafide certificates and leave applications. */
@Service
public class RequestService {

    private static final Map<String, String> LABEL = Map.of(
            "TRANSCRIPT", "official transcript", "ID_REISSUE", "ID card re-issue",
            "BONAFIDE", "bonafide certificate", "LEAVE", "leave application");

    private final StudentRequestRepository requests;
    private final UserRepository users;
    private final NotificationService notifications;

    public RequestService(StudentRequestRepository requests, UserRepository users, NotificationService notifications) {
        this.requests = requests;
        this.users = users;
        this.notifications = notifications;
    }

    public StudentRequest create(User student, RequestCreate r) {
        String details = r.details() == null ? "" : r.details().trim();

        if ("LEAVE".equals(r.type())) {
            if (r.leaveFrom() == null || r.leaveTo() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Choose the first and last day of your leave");
            }
            if (r.leaveTo().isBefore(r.leaveFrom())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "The last day cannot be before the first day");
            }
            if (details.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Please give a reason for your leave");
            }
        } else if (requests.existsByStudentIdAndTypeAndStatus(student.getId(), r.type(), StudentRequest.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a pending " + LABEL.get(r.type()) + " request");
        }

        StudentRequest q = new StudentRequest();
        q.setStudentId(student.getId());
        q.setType(r.type());
        q.setDetails(details);
        if ("LEAVE".equals(r.type())) {
            q.setLeaveFrom(r.leaveFrom());
            q.setLeaveTo(r.leaveTo());
        }
        q = requests.save(q);

        notifications.notifyAdmins("SYSTEM", "New request: " + LABEL.get(r.type()),
                (student.getFirstName() + " " + student.getLastName()).trim() + " submitted a " + LABEL.get(r.type()) + ".", null);
        return q;
    }

    public List<StudentRequest> mine(String studentId) {
        return requests.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    public List<AdminRequestView> list(String status) {
        List<StudentRequest> rows = status == null || status.isBlank()
                ? requests.findAllByOrderByCreatedAtDesc() : requests.findByStatusOrderByCreatedAtDesc(status);
        Map<String, User> byId = users.findAllById(rows.stream().map(StudentRequest::getStudentId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return rows.stream().map(q -> {
            User u = byId.get(q.getStudentId());
            return new AdminRequestView(q.getId(), q.getStudentId(),
                    u == null ? "Unknown" : (u.getFirstName() + " " + u.getLastName()).trim(),
                    u == null ? "" : u.getFaculty(), q.getType(), q.getDetails(), q.getLeaveFrom(), q.getLeaveTo(),
                    q.getStatus(), q.getAdminNote(), q.getDecidedBy(), q.getCreatedAt(), q.getDecidedAt());
        }).toList();
    }

    public void decide(String id, String adminEmail, RequestDecision d) {
        StudentRequest q = requests.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!StudentRequest.PENDING.equals(q.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This request was already decided");
        }
        q.setStatus(d.approve() ? StudentRequest.APPROVED : StudentRequest.REJECTED);
        q.setAdminNote(d.note() == null ? "" : d.note().trim());
        q.setDecidedBy(adminEmail);
        q.setDecidedAt(Instant.now());
        requests.save(q);

        String next = switch (q.getType()) {
            case "TRANSCRIPT" -> " You can print your official transcript from the Transcript page.";
            case "BONAFIDE" -> " You can print your certificate from the Requests page.";
            case "ID_REISSUE" -> " Please collect your new ID card from the office.";
            default -> "";
        };
        notifications.notify(q.getStudentId(), "SYSTEM",
                "Your " + LABEL.get(q.getType()) + " was " + (d.approve() ? "approved" : "declined"),
                (d.approve() ? next.trim() : "") + (q.getAdminNote().isEmpty() ? "" : " Note: " + q.getAdminNote()), null);
    }

    public CertificateView certificate(User student, String requestId) {
        StudentRequest q = requests.findById(requestId)
                .filter(x -> x.getStudentId().equals(student.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!"BONAFIDE".equals(q.getType()) || !StudentRequest.APPROVED.equals(q.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No certificate is available for this request");
        }
        String name = (student.getFirstName() + " " + student.getLastName()).trim();
        String text = "This is to certify that " + name + " is a bona fide student of TecSpace, enrolled in the "
                + student.getFaculty() + " programme (" + student.getSession() + " session)."
                + (q.getDetails().isEmpty() ? "" : " This certificate is issued for the purpose of: " + q.getDetails() + ".");
        return new CertificateView("BON-" + q.getId().substring(Math.max(0, q.getId().length() - 8)).toUpperCase(),
                "Bonafide certificate", name, student.getFaculty(), student.getSession(), text, LocalDate.now());
    }
}
