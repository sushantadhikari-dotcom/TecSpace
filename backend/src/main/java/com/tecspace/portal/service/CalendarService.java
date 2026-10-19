package com.tecspace.portal.service;

import com.tecspace.portal.dto.CalendarRequest;
import com.tecspace.portal.dto.CalendarView;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.CalendarEvent;
import com.tecspace.portal.repository.CalendarEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** The academic calendar. REGISTRATION events are the windows when students may ask for courses. */
@Service
public class CalendarService {

    private final CalendarEventRepository events;

    public CalendarService(CalendarEventRepository events) {
        this.events = events;
    }

    public List<CalendarEvent> list() {
        return events.findAllByOrderByStartDateAsc();
    }

    public CalendarEvent create(CalendarRequest r) {
        return events.save(apply(new CalendarEvent(), r));
    }

    public CalendarEvent update(String id, CalendarRequest r) {
        CalendarEvent e = events.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
        return events.save(apply(e, r));
    }

    public void delete(String id) {
        if (!events.existsById(id)) throw new ApiException(HttpStatus.NOT_FOUND, "Event not found");
        events.deleteById(id);
    }

    public CalendarView view() {
        List<CalendarEvent> all = list();
        Status s = status();
        return new CalendarView(all, s.open(), s.message());
    }

    /** If the administration set no registration window at all, registration is always open. */
    public void requireRegistrationOpen() {
        Status s = status();
        if (!s.open()) throw new ApiException(HttpStatus.FORBIDDEN, s.message());
    }

    private record Status(boolean open, String message) {}

    private Status status() {
        List<CalendarEvent> windows = events.findByType("REGISTRATION");
        if (windows.isEmpty()) return new Status(true, "");

        LocalDate today = LocalDate.now();
        for (CalendarEvent w : windows) {
            if (!today.isBefore(w.getStartDate()) && !today.isAfter(w.getEndDate())) {
                return new Status(true, "Course registration is open until " + w.getEndDate() + ".");
            }
        }
        return windows.stream().filter(w -> w.getStartDate().isAfter(today))
                .min(Comparator.comparing(CalendarEvent::getStartDate))
                .map(w -> new Status(false, "Course registration is closed. It opens on " + w.getStartDate() + "."))
                .orElse(new Status(false, "Course registration is closed."));
    }

    private CalendarEvent apply(CalendarEvent e, CalendarRequest r) {
        if (r.endDate().isBefore(r.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The end date cannot be before the start date");
        }
        e.setTitle(r.title().trim());
        e.setType(r.type());
        e.setStartDate(r.startDate());
        e.setEndDate(r.endDate());
        e.setNote(r.note() == null ? "" : r.note().trim());
        return e;
    }
}
