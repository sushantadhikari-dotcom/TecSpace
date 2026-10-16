package com.tecspace.portal.service;

import com.tecspace.portal.dto.ExamEntry;
import com.tecspace.portal.dto.ExamRequest;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.CourseOffering;
import com.tecspace.portal.model.Enrollment;
import com.tecspace.portal.model.ExamSlot;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.CourseOfferingRepository;
import com.tecspace.portal.repository.EnrollmentRepository;
import com.tecspace.portal.repository.ExamSlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExamService {

    private static final Pattern TIME = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final ExamSlotRepository exams;
    private final CourseOfferingService courseService;
    private final CourseOfferingRepository courses;
    private final EnrollmentRepository enrollments;
    private final NotificationService notifications;

    public ExamService(ExamSlotRepository exams, CourseOfferingService courseService,
                       CourseOfferingRepository courses, EnrollmentRepository enrollments,
                       NotificationService notifications) {
        this.exams = exams;
        this.courseService = courseService;
        this.courses = courses;
        this.enrollments = enrollments;
        this.notifications = notifications;
    }

    public ExamEntry add(ExamRequest r) {
        String start = r.startTime().trim();
        String end = r.endTime().trim();
        String room = r.room().trim();
        if (!TIME.matcher(start).matches() || !TIME.matcher(end).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Times must look like 09:00");
        }
        if (start.compareTo(end) >= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }

        CourseOffering course = courseService.get(r.courseCode());
        Set<String> mine = enrolledIds(course.getCode());

        for (ExamSlot other : exams.findByDate(r.date())) {
            if (!TimetableService.overlaps(start, end, other.getStartTime(), other.getEndTime())) continue;
            String when = other.getCourseCode() + " exam, " + other.getStartTime() + "-" + other.getEndTime();

            if (other.getCourseCode().equals(course.getCode())) {
                throw new ApiException(HttpStatus.CONFLICT, "This course already has an exam at that time");
            }
            if (other.getRoom().equalsIgnoreCase(room)) {
                throw new ApiException(HttpStatus.CONFLICT, "Room " + room + " is already used by the " + when);
            }
            Set<String> shared = new HashSet<>(enrolledIds(other.getCourseCode()));
            shared.retainAll(mine);
            if (!shared.isEmpty()) {
                throw new ApiException(HttpStatus.CONFLICT, shared.size() + " student(s) are enrolled in both "
                        + course.getCode() + " and " + other.getCourseCode() + ", so these exams would overlap (" + when + ")");
            }
        }

        ExamSlot e = new ExamSlot();
        e.setCourseCode(course.getCode());
        e.setDate(r.date());
        e.setStartTime(start);
        e.setEndTime(end);
        e.setRoom(room);
        e.setNote(r.note() == null ? "" : r.note().trim());
        e = exams.save(e);

        notifications.notifyAll(mine, "EXAM", "Exam scheduled: " + course.getCode(),
                course.getName() + " exam on " + e.getDate() + ", " + start + "-" + end + ", room " + room + ".", null);
        return toEntry(e, Map.of(course.getCode(), course));
    }

    public void delete(String id) {
        ExamSlot e = exams.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Exam not found"));
        exams.delete(e);
        notifications.notifyAll(enrolledIds(e.getCourseCode()), "EXAM", "Exam cancelled: " + e.getCourseCode(),
                "The " + e.getCourseCode() + " exam on " + e.getDate() + " was removed from the timetable.", null);
    }

    public List<ExamEntry> all() {
        return toEntries(exams.findAll());
    }

    public List<ExamEntry> forStudent(User student) {
        Set<String> codes = enrollments
                .findByStudentIdAndTermAndStatus(student.getId(), courseService.term(), Enrollment.ENROLLED)
                .stream().map(Enrollment::getCourseCode).collect(Collectors.toSet());
        if (codes.isEmpty()) return List.of();
        return toEntries(exams.findByCourseCodeIn(codes));
    }

    // ---------------------------------------------------------------- helpers

    private Set<String> enrolledIds(String courseCode) {
        return enrollments.findByCourseCodeAndTermAndStatus(courseCode, courseService.term(), Enrollment.ENROLLED)
                .stream().map(Enrollment::getStudentId).collect(Collectors.toSet());
    }

    private List<ExamEntry> toEntries(List<ExamSlot> list) {
        Map<String, CourseOffering> byCode = courses.findAll().stream()
                .collect(Collectors.toMap(CourseOffering::getCode, c -> c, (a, b) -> a));
        return list.stream()
                .sorted(Comparator.comparing(ExamSlot::getDate).thenComparing(ExamSlot::getStartTime))
                .map(e -> toEntry(e, byCode))
                .toList();
    }

    private ExamEntry toEntry(ExamSlot e, Map<String, CourseOffering> byCode) {
        CourseOffering c = byCode.get(e.getCourseCode());
        return new ExamEntry(e.getId(), e.getCourseCode(), c == null ? e.getCourseCode() : c.getName(),
                e.getDate(), e.getStartTime(), e.getEndTime(), e.getRoom(), e.getNote());
    }
}
