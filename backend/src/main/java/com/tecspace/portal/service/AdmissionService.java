package com.tecspace.portal.service;

import com.tecspace.portal.dto.*;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.Application;
import com.tecspace.portal.model.ApplicationDocument;
import com.tecspace.portal.model.SeatConfig;
import com.tecspace.portal.repository.ApplicationRepository;
import com.tecspace.portal.repository.SeatConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Applications, document checks, approval with automatic seat allocation, and enrolment into a student account. */
@Service
public class AdmissionService {

    private static final List<String> OPEN = List.of(Application.SUBMITTED, Application.APPROVED, Application.WAITLISTED);
    private static final List<String> TAKING_SEAT = List.of(Application.APPROVED, Application.ENROLLED);
    private static final List<String> FACULTIES = List.of("BSIT", "BBA", "MSCIT");
    private static final List<String> SESSIONS = List.of("SPRING", "FALL", "SUMMER");
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private final ApplicationRepository applications;
    private final SeatConfigRepository seatConfigs;
    private final AuthService authService;
    private final NotificationService notifications;
    private final SecureRandom random = new SecureRandom();

    public AdmissionService(ApplicationRepository applications, SeatConfigRepository seatConfigs,
                            AuthService authService, NotificationService notifications) {
        this.applications = applications;
        this.seatConfigs = seatConfigs;
        this.authService = authService;
        this.notifications = notifications;
    }

    // ---------------------------------------------------------------- applicant (public)

    public TrackView apply(ApplyRequest r) {
        String email = r.email().trim().toLowerCase();
        if (applications.existsByEmailAndFacultyAndSessionAndStatusIn(email, r.faculty(), r.session(), OPEN)) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have an open application for this programme and session");
        }

        Application a = new Application();
        a.setTrackingCode(newCode());
        a.setFirstName(r.firstName().trim());
        a.setLastName(r.lastName().trim());
        a.setEmail(email);
        a.setMobileNumber(r.mobileNumber());
        a.setFaculty(r.faculty());
        a.setSession(r.session());
        a.setPreviousSchool(r.previousSchool().trim());
        a.setLastScore(r.lastScore());
        a.setStatement(r.statement() == null ? "" : r.statement().trim());
        a.setDocuments(new ArrayList<>(List.of(
                new ApplicationDocument("ID proof", "PENDING", ""),
                new ApplicationDocument("Previous academic transcript", "PENDING", ""),
                new ApplicationDocument("Passport-size photo", "PENDING", ""))));
        a = applications.save(a);

        notifications.notifyAdmins("SYSTEM", "New application: " + a.getFirstName() + " " + a.getLastName(),
                a.getFaculty() + " · " + a.getSession() + " (" + a.getTrackingCode() + ")", null);
        return toTrack(a);
    }

    /** The applicant needs both the tracking code and the email, so codes cannot be guessed. */
    public TrackView track(String code, String email) {
        Application a = applications.findByTrackingCode(code.trim().toUpperCase())
                .filter(x -> x.getEmail().equalsIgnoreCase(email.trim()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No application found with that code and email"));
        return toTrack(a);
    }

    // ---------------------------------------------------------------- admin

    public List<Application> list(String status) {
        return status == null || status.isBlank()
                ? applications.findAllByOrderBySubmittedAtDesc()
                : applications.findByStatusOrderBySubmittedAtDesc(status);
    }

    public Application verifyDocuments(String id, List<DocUpdate> updates) {
        Application a = get(id);
        for (DocUpdate u : updates) {
            ApplicationDocument d = a.getDocuments().stream().filter(x -> x.getName().equals(u.name())).findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown document: " + u.name()));
            d.setStatus(u.status());
            d.setNote(u.note() == null ? "" : u.note().trim());
        }
        return applications.save(a);
    }

    public Application decide(String id, String adminEmail, DecisionRequest r) {
        Application a = get(id);
        String remarks = r.remarks() == null ? "" : r.remarks().trim();
        String before = a.getStatus();

        if (Application.ENROLLED.equals(before) || Application.REJECTED.equals(before)) {
            throw new ApiException(HttpStatus.CONFLICT, "This application is already " + before.toLowerCase());
        }

        if ("APPROVE".equals(r.decision())) {
            if (Application.APPROVED.equals(before)) {
                throw new ApiException(HttpStatus.CONFLICT, "This application is already approved");
            }
            boolean allVerified = a.getDocuments().stream().allMatch(d -> "VERIFIED".equals(d.getStatus()));
            if (!allVerified) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Verify every document before approving");
            }
            if (seatAvailable(a.getFaculty(), a.getSession())) {
                a.setStatus(Application.APPROVED);
            } else {
                a.setStatus(Application.WAITLISTED);
                remarks = remarks.isEmpty() ? "No seats are free right now. You are on the waiting list." : remarks;
            }
        } else {
            a.setStatus(Application.REJECTED);
        }

        a.setRemarks(remarks);
        a.setDecidedBy(adminEmail);
        a.setDecidedAt(Instant.now());
        a = applications.save(a);

        // A rejected, approved applicant frees a seat for the waiting list
        if (Application.REJECTED.equals(a.getStatus()) && Application.APPROVED.equals(before)) {
            promoteWaitlist(a.getFaculty(), a.getSession());
        }
        return a;
    }

    /** Creates the student account. Returns the temporary password to hand to the new student. */
    public CreatedUser enroll(String id, String universityEmail) {
        Application a = get(id);
        if (!Application.APPROVED.equals(a.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only approved applications can be enrolled");
        }
        String temp = TempPassword.generate();
        AuthResponse created = authService.signupTemporary(new SignupRequest(universityEmail.trim(), temp, a.getFirstName(),
                a.getLastName(), a.getMobileNumber(), a.getFaculty(), a.getSession()));

        a.setStatus(Application.ENROLLED);
        a.setStudentId(created.id());
        a.setDecidedAt(Instant.now());
        applications.save(a);

        return new CreatedUser(new UserView(created.id(), created.universityEmail(), created.firstName(),
                created.lastName(), a.getMobileNumber(), created.role(), List.of(), true, Instant.now()), temp);
    }

    // ---------------------------------------------------------------- seats

    public List<SeatStatus> seatStatus() {
        List<SeatStatus> out = new ArrayList<>();
        for (String f : FACULTIES) {
            for (String s : SESSIONS) {
                Integer seats = seatConfigs.findByFacultyAndSession(f, s).map(SeatConfig::getSeats).orElse(null);
                int allocated = (int) applications.countByFacultyAndSessionAndStatusIn(f, s, TAKING_SEAT);
                int waiting = (int) applications.countByFacultyAndSessionAndStatusIn(f, s, List.of(Application.WAITLISTED));
                out.add(new SeatStatus(f, s, seats, allocated, waiting));
            }
        }
        return out;
    }

    public void setSeats(SeatConfigRequest r) {
        SeatConfig c = seatConfigs.findByFacultyAndSession(r.faculty(), r.session()).orElseGet(SeatConfig::new);
        c.setFaculty(r.faculty());
        c.setSession(r.session());
        c.setSeats(r.seats());
        seatConfigs.save(c);
        promoteWaitlist(r.faculty(), r.session());   // more seats may let waiting applicants in
    }

    // ---------------------------------------------------------------- helpers

    /** No limit set = no limit. */
    private boolean seatAvailable(String faculty, String session) {
        return seatConfigs.findByFacultyAndSession(faculty, session)
                .map(c -> applications.countByFacultyAndSessionAndStatusIn(faculty, session, TAKING_SEAT) < c.getSeats())
                .orElse(true);
    }

    /** Moves waiting applicants (oldest first, documents verified) into free seats. */
    private void promoteWaitlist(String faculty, String session) {
        for (Application w : applications.findByFacultyAndSessionAndStatusOrderBySubmittedAtAsc(
                faculty, session, Application.WAITLISTED)) {
            if (!seatAvailable(faculty, session)) return;
            if (!w.getDocuments().stream().allMatch(d -> "VERIFIED".equals(d.getStatus()))) continue;
            w.setStatus(Application.APPROVED);
            w.setRemarks("A seat became available and your application is now approved.");
            w.setDecidedAt(Instant.now());
            applications.save(w);
        }
    }

    private Application get(String id) {
        return applications.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private TrackView toTrack(Application a) {
        return new TrackView(a.getTrackingCode(), a.getFirstName() + " " + a.getLastName(), a.getFaculty(),
                a.getSession(), a.getStatus(), a.getRemarks(), a.getDocuments(), a.getSubmittedAt());
    }

    private String newCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("APP-");
            for (int i = 0; i < 6; i++) sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            code = sb.toString();
        } while (applications.findByTrackingCode(code).isPresent());
        return code;
    }
}
