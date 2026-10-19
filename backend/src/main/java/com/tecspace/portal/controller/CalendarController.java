package com.tecspace.portal.controller;

import com.tecspace.portal.dto.CalendarRequest;
import com.tecspace.portal.dto.CalendarView;
import com.tecspace.portal.model.CalendarEvent;
import com.tecspace.portal.service.CalendarService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Everyone logged in can read the calendar. Staff with the right permission can change it. */
@RestController
@RequestMapping("/api")
public class CalendarController {

    private final CalendarService calendar;

    public CalendarController(CalendarService calendar) {
        this.calendar = calendar;
    }

    @GetMapping("/calendar")
    public CalendarView view() {
        return calendar.view();
    }

    @PostMapping("/admin/calendar")
    public ResponseEntity<CalendarEvent> create(@Valid @RequestBody CalendarRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(calendar.create(req));
    }

    @PutMapping("/admin/calendar/{id}")
    public CalendarEvent update(@PathVariable String id, @Valid @RequestBody CalendarRequest req) {
        return calendar.update(id, req);
    }

    @DeleteMapping("/admin/calendar/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        calendar.delete(id);
        return ResponseEntity.noContent().build();
    }
}
