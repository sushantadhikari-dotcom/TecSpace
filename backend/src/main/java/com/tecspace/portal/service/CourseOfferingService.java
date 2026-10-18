package com.tecspace.portal.service;

import com.tecspace.portal.dto.CourseRequest;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.CourseOffering;
import com.tecspace.portal.model.Enrollment;
import com.tecspace.portal.repository.CourseOfferingRepository;
import com.tecspace.portal.repository.EnrollmentRepository;
import com.tecspace.portal.repository.TimetableSlotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseOfferingService {

    private final CourseOfferingRepository courses;
    private final EnrollmentRepository enrollments;
    private final TimetableSlotRepository slots;

    @Value("${tecspace.current-term:Fall 2026}")
    private String currentTerm;

    public CourseOfferingService(CourseOfferingRepository courses, EnrollmentRepository enrollments,
                                 TimetableSlotRepository slots) {
        this.courses = courses;
        this.enrollments = enrollments;
        this.slots = slots;
    }

    public String term() {
        return currentTerm;
    }

    public List<CourseOffering> list() {
        return courses.findByTerm(currentTerm);
    }

    public CourseOffering get(String code) {
        return courses.findByCode(normalize(code))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course " + code + " not found"));
    }

    public CourseOffering create(CourseRequest r) {
        String code = normalize(r.code());
        if (courses.existsByCode(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "A course with code " + code + " already exists");
        }
        CourseOffering c = new CourseOffering();
        c.setCode(code);
        c.setTerm(currentTerm);
        apply(c, r);
        return courses.save(c);
    }

    public CourseOffering update(String code, CourseRequest r) {
        CourseOffering c = get(code);
        long enrolled = enrollments.countByCourseCodeAndTermAndStatus(c.getCode(), c.getTerm(), Enrollment.ENROLLED);
        if (r.capacity() < enrolled) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Capacity cannot be lower than the " + enrolled + " students already enrolled");
        }
        apply(c, r);
        return courses.save(c);
    }

    public void delete(String code) {
        CourseOffering c = get(code);
        if (enrollments.countByCourseCodeAndStatus(c.getCode(), Enrollment.ENROLLED) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Students are enrolled in this course. Remove them first.");
        }
        slots.deleteByCourseCode(c.getCode());
        courses.delete(c);
    }

    private void apply(CourseOffering c, CourseRequest r) {
        c.setName(r.name().trim());
        c.setDepartment(r.department().trim());
        c.setCreditHours(r.creditHours());
        c.setCapacity(r.capacity());
        c.setInstructorName(r.instructorName() == null ? "" : r.instructorName().trim());
        c.setSyllabus(r.syllabus() == null ? "" : r.syllabus().trim());
        String url = r.syllabusUrl() == null ? "" : r.syllabusUrl().trim();
        if (!url.isEmpty() && !(url.startsWith("http://") || url.startsWith("https://"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The syllabus link must start with http:// or https://");
        }
        c.setSyllabusUrl(url);

        List<String> prereqs = new ArrayList<>();
        if (r.prerequisites() != null) {
            for (String p : r.prerequisites()) {
                String pc = normalize(p);
                if (pc.isEmpty()) continue;
                if (pc.equals(c.getCode())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "A course cannot be its own prerequisite");
                }
                if (!prereqs.contains(pc)) prereqs.add(pc);
            }
        }
        c.setPrerequisites(prereqs);
    }

    private String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }
}
