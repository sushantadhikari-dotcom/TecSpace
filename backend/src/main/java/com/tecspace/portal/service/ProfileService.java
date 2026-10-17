package com.tecspace.portal.service;

import com.tecspace.portal.dto.HistoryRow;
import com.tecspace.portal.dto.ProfileData;
import com.tecspace.portal.model.*;
import com.tecspace.portal.repository.CourseOfferingRepository;
import com.tecspace.portal.repository.EnrollmentRepository;
import com.tecspace.portal.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Personal details, emergency contacts, medical information and enrolment history. */
@Service
public class ProfileService {

    private final StudentProfileRepository profiles;
    private final EnrollmentRepository enrollments;
    private final CourseOfferingRepository courses;

    public ProfileService(StudentProfileRepository profiles, EnrollmentRepository enrollments,
                          CourseOfferingRepository courses) {
        this.profiles = profiles;
        this.enrollments = enrollments;
        this.courses = courses;
    }

    public ProfileData get(String studentId) {
        return profiles.findByStudentId(studentId).map(this::toData)
                .orElseGet(() -> new ProfileData(null, "", "", "", List.of(),
                        new ProfileData.Medical("", "", "", "", "")));
    }

    public ProfileData update(String studentId, ProfileData d, String actorEmail) {
        StudentProfile p = profiles.findByStudentId(studentId).orElseGet(StudentProfile::new);
        p.setStudentId(studentId);
        p.setDateOfBirth(d.dateOfBirth());
        p.setGender(blank(d.gender()));
        p.setAddress(blank(d.address()));
        p.setNationality(blank(d.nationality()));

        List<EmergencyContact> contacts = new ArrayList<>();
        if (d.emergencyContacts() != null) {
            for (ProfileData.Contact c : d.emergencyContacts()) {
                contacts.add(new EmergencyContact(c.name().trim(), c.relationship().trim(), c.phone()));
            }
        }
        p.setEmergencyContacts(contacts);

        ProfileData.Medical m = d.medical();
        p.setMedical(m == null ? new MedicalInfo() : new MedicalInfo(blank(m.bloodGroup()), blank(m.allergies()),
                blank(m.conditions()), blank(m.medications()), blank(m.notes())));
        p.setUpdatedAt(Instant.now());
        p.setUpdatedBy(actorEmail);
        return toData(profiles.save(p));
    }

    /** Every course the student ever registered for, newest first, including dropped ones. */
    public List<HistoryRow> history(String studentId) {
        Map<String, String> names = courses.findAll().stream()
                .collect(Collectors.toMap(CourseOffering::getCode, CourseOffering::getName, (a, b) -> a));
        return enrollments.findByStudentIdOrderByEnrolledAtDesc(studentId).stream()
                .map(e -> new HistoryRow(e.getCourseCode(), names.getOrDefault(e.getCourseCode(), e.getCourseCode()),
                        e.getTerm(), e.getStatus(), e.getEnrolledAt()))
                .toList();
    }

    private ProfileData toData(StudentProfile p) {
        List<ProfileData.Contact> contacts = p.getEmergencyContacts().stream()
                .map(c -> new ProfileData.Contact(c.getName(), c.getRelationship(), c.getPhone())).toList();
        MedicalInfo m = p.getMedical() == null ? new MedicalInfo() : p.getMedical();
        return new ProfileData(p.getDateOfBirth(), p.getGender(), p.getAddress(), p.getNationality(), contacts,
                new ProfileData.Medical(m.getBloodGroup(), m.getAllergies(), m.getConditions(), m.getMedications(), m.getNotes()));
    }

    private String blank(String s) {
        return s == null ? "" : s.trim();
    }
}
