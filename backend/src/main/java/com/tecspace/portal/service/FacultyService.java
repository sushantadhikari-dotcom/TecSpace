package com.tecspace.portal.service;

import com.tecspace.portal.dto.FacultyAssignmentView;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.FacultyAssignment;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.FacultyAssignmentRepository;
import com.tecspace.portal.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/** Which teacher is responsible for which course. */
@Service
public class FacultyService {

    private final FacultyAssignmentRepository assignments;
    private final UserRepository users;
    private final CourseOfferingService courseService;

    public FacultyService(FacultyAssignmentRepository assignments, UserRepository users,
                          CourseOfferingService courseService) {
        this.assignments = assignments;
        this.users = users;
        this.courseService = courseService;
    }

    public List<FacultyAssignmentView> all() {
        List<FacultyAssignmentView> out = new ArrayList<>();
        for (User t : users.findByRole("ROLE_TEACHER")) {
            out.add(new FacultyAssignmentView(t.getId(), (t.getFirstName() + " " + t.getLastName()).trim(),
                    t.getUniversityEmail(), codesFor(t.getId()).stream().sorted().toList()));
        }
        return out;
    }

    public void assign(String teacherId, List<String> courseCodes) {
        User t = users.findById(teacherId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Teacher not found"));
        if (!"ROLE_TEACHER".equals(t.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only teacher accounts can be assigned to courses");
        }
        Set<String> codes = new LinkedHashSet<>();
        for (String c : courseCodes) codes.add(courseService.get(c).getCode());   // throws if the course does not exist

        assignments.deleteByTeacherId(teacherId);
        for (String code : codes) {
            FacultyAssignment a = new FacultyAssignment();
            a.setTeacherId(teacherId);
            a.setCourseCode(code);
            assignments.save(a);
        }
    }

    public Set<String> codesFor(String teacherId) {
        return assignments.findByTeacherId(teacherId).stream()
                .map(FacultyAssignment::getCourseCode).collect(Collectors.toCollection(TreeSet::new));
    }

    public List<String> teacherIdsFor(String courseCode) {
        return assignments.findByCourseCode(courseCode).stream().map(FacultyAssignment::getTeacherId).toList();
    }

    public void requireAssigned(String teacherId, String courseCode) {
        String code = courseCode == null ? "" : courseCode.trim().toUpperCase();
        if (!codesFor(teacherId).contains(code)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not assigned to " + code);
        }
    }
}