package com.tecspace.portal.service;

import com.tecspace.portal.dto.ClassEntry;
import com.tecspace.portal.dto.SlotRequest;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.CourseOffering;
import com.tecspace.portal.model.TimetableSlot;
import com.tecspace.portal.repository.CourseOfferingRepository;
import com.tecspace.portal.repository.TimetableSlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TimetableService {

    public static final List<String> DAYS =
            List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
    private static final Pattern TIME = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");

    private final TimetableSlotRepository slots;
    private final CourseOfferingRepository courses;

    public TimetableService(TimetableSlotRepository slots, CourseOfferingRepository courses) {
        this.slots = slots;
        this.courses = courses;
    }

    /** Two time ranges on the same day overlap. Times are zero-padded "HH:mm", so text comparison works. */
    public static boolean overlaps(String start1, String end1, String start2, String end2) {
        return start1.compareTo(end2) < 0 && start2.compareTo(end1) < 0;
    }

    public TimetableSlot add(SlotRequest r) {
        String day = r.day().trim().toUpperCase();
        String start = r.startTime().trim();
        String end = r.endTime().trim();
        String room = r.room().trim();

        if (!DAYS.contains(day)) throw new ApiException(HttpStatus.BAD_REQUEST, "Day must be MONDAY to SUNDAY");
        if (!TIME.matcher(start).matches() || !TIME.matcher(end).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Times must look like 09:00");
        }
        if (start.compareTo(end) >= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "End time must be after start time");

        CourseOffering course = courses.findByCode(r.courseCode().trim().toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found"));

        // Clash detection against every class on that day
        for (TimetableSlot other : slots.findByDay(day)) {
            if (!overlaps(start, end, other.getStartTime(), other.getEndTime())) continue;
            String when = day + " " + other.getStartTime() + "-" + other.getEndTime();

            if (other.getRoom().equalsIgnoreCase(room)) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "Room " + room + " is already booked for " + other.getCourseCode() + " on " + when);
            }
            if (other.getCourseCode().equals(course.getCode())) {
                throw new ApiException(HttpStatus.CONFLICT, "This course already has a class on " + when);
            }
            String teacher = course.getInstructorName();
            if (teacher != null && !teacher.isBlank()) {
                String otherTeacher = courses.findByCode(other.getCourseCode())
                        .map(CourseOffering::getInstructorName).orElse("");
                if (teacher.equalsIgnoreCase(otherTeacher)) {
                    throw new ApiException(HttpStatus.CONFLICT,
                            teacher + " is already teaching " + other.getCourseCode() + " on " + when);
                }
            }
        }

        TimetableSlot s = new TimetableSlot();
        s.setCourseCode(course.getCode());
        s.setDay(day);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setRoom(room);
        return slots.save(s);
    }

    public void delete(String id) {
        if (!slots.existsById(id)) throw new ApiException(HttpStatus.NOT_FOUND, "Class slot not found");
        slots.deleteById(id);
    }

    public List<ClassEntry> all() {
        return toEntries(slots.findAll());
    }

    public List<ClassEntry> forCourses(Collection<String> codes) {
        if (codes.isEmpty()) return List.of();
        return toEntries(slots.findByCourseCodeIn(codes));
    }

    private List<ClassEntry> toEntries(List<TimetableSlot> list) {
        Map<String, CourseOffering> byCode = courses.findAll().stream()
                .collect(Collectors.toMap(CourseOffering::getCode, c -> c, (a, b) -> a));
        return list.stream()
                .map(s -> {
                    CourseOffering c = byCode.get(s.getCourseCode());
                    return new ClassEntry(s.getId(), s.getCourseCode(),
                            c == null ? s.getCourseCode() : c.getName(),
                            c == null ? "" : c.getInstructorName(),
                            s.getDay(), s.getStartTime(), s.getEndTime(), s.getRoom());
                })
                .sorted(Comparator.comparingInt((ClassEntry e) -> DAYS.indexOf(e.day()))
                        .thenComparing(ClassEntry::startTime))
                .toList();
    }
}
