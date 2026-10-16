package com.tecspace.portal.controller;

import com.tecspace.portal.dto.AnnouncementRequest;
import com.tecspace.portal.dto.ClassEntry;
import com.tecspace.portal.dto.CourseRequest;
import com.tecspace.portal.dto.SlotRequest;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.Announcement;
import com.tecspace.portal.model.CourseOffering;
import com.tecspace.portal.repository.AnnouncementRepository;
import com.tecspace.portal.service.CourseOfferingService;
import com.tecspace.portal.service.TimetableService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminAcademicController {

    private final CourseOfferingService courseService;
    private final TimetableService timetableService;
    private final AnnouncementRepository announcements;

    public AdminAcademicController(CourseOfferingService courseService, TimetableService timetableService,
                                   AnnouncementRepository announcements) {
        this.courseService = courseService;
        this.timetableService = timetableService;
        this.announcements = announcements;
    }

    // ---- courses
    @GetMapping("/courses")
    public List<CourseOffering> courses() {
        return courseService.list();
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseOffering> createCourse(@Valid @RequestBody CourseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(req));
    }

    @PutMapping("/courses/{code}")
    public CourseOffering updateCourse(@PathVariable String code, @Valid @RequestBody CourseRequest req) {
        return courseService.update(code, req);
    }

    @DeleteMapping("/courses/{code}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String code) {
        courseService.delete(code);
        return ResponseEntity.noContent().build();
    }

    // ---- timetable
    @GetMapping("/timetable")
    public List<ClassEntry> timetable() {
        return timetableService.all();
    }

    @PostMapping("/timetable")
    public ResponseEntity<ClassEntry> addSlot(@Valid @RequestBody SlotRequest req) {
        var slot = timetableService.add(req);
        ClassEntry entry = timetableService.all().stream()
                .filter(e -> e.id().equals(slot.getId())).findFirst().orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @DeleteMapping("/timetable/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable String id) {
        timetableService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- announcements
    @GetMapping("/announcements")
    public List<Announcement> listAnnouncements() {
        return announcements.findTop20ByOrderByCreatedAtDesc();
    }

    @PostMapping("/announcements")
    public ResponseEntity<Announcement> postAnnouncement(Authentication auth,
                                                         @Valid @RequestBody AnnouncementRequest req) {
        Announcement a = new Announcement();
        a.setTitle(req.title().trim());
        a.setMessage(req.message().trim());
        a.setPostedBy(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(announcements.save(a));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable String id) {
        if (!announcements.existsById(id)) throw new ApiException(HttpStatus.NOT_FOUND, "Announcement not found");
        announcements.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
