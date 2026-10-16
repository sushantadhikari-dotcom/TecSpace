package com.tecspace.portal.service;

import com.tecspace.portal.dto.TermStatus;
import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.GradeRecord;
import com.tecspace.portal.model.ResultTermState;
import com.tecspace.portal.repository.GradeRepository;
import com.tecspace.portal.repository.ResultTermStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/** Gradebook locking and report card publishing, per trimester name. */
@Service
public class ResultTermService {

    private final ResultTermStateRepository states;
    private final GradeRepository grades;
    private final NotificationService notifications;

    public ResultTermService(ResultTermStateRepository states, GradeRepository grades, NotificationService notifications) {
        this.states = states;
        this.grades = grades;
        this.notifications = notifications;
    }

    public List<TermStatus> list() {
        Map<String, Integer> counts = new TreeMap<>();
        for (GradeRecord g : grades.findAll()) {
            g.getTrimesters().forEach(t -> counts.merge(t.getTrimesterName(), 1, Integer::sum));
        }
        List<TermStatus> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            ResultTermState s = states.findByTrimesterName(e.getKey()).orElse(null);
            out.add(new TermStatus(e.getKey(), e.getValue(), s != null && s.isLocked(), s != null && s.isPublished()));
        }
        return out;
    }

    public void lock(String name) {
        ResultTermState s = stateOf(name);
        s.setLocked(true);
        save(s);
    }

    public void unlock(String name) {
        ResultTermState s = stateOf(name);
        if (s.isPublished()) {
            throw new ApiException(HttpStatus.CONFLICT, "Unpublish this trimester before unlocking it");
        }
        s.setLocked(false);
        save(s);
    }

    /** Publishing also locks the grades, so a published report card cannot silently change. */
    public void publish(String name) {
        ResultTermState s = stateOf(name);
        s.setLocked(true);
        s.setPublished(true);
        save(s);

        List<String> studentIds = grades.findAll().stream()
                .filter(g -> g.getTrimesters().stream().anyMatch(t -> t.getTrimesterName().equals(name)))
                .map(GradeRecord::getStudentId).toList();
        notifications.notifyAll(studentIds, "SYSTEM", "Results published: " + name,
                "Your report card for " + name + " is now available in Transcript.", null);
    }

    public void unpublish(String name) {
        ResultTermState s = stateOf(name);
        s.setPublished(false);
        save(s);
    }

    public Set<String> publishedNames() {
        Set<String> names = new HashSet<>();
        states.findByPublishedTrue().forEach(s -> names.add(s.getTrimesterName()));
        return names;
    }

    private ResultTermState stateOf(String name) {
        String n = name.trim();
        boolean exists = grades.findAll().stream()
                .anyMatch(g -> g.getTrimesters().stream().anyMatch(t -> t.getTrimesterName().equals(n)));
        if (!exists) throw new ApiException(HttpStatus.NOT_FOUND, "No student has a trimester called \"" + n + "\"");
        return states.findByTrimesterName(n).orElseGet(() -> {
            ResultTermState s = new ResultTermState();
            s.setTrimesterName(n);
            return s;
        });
    }

    private void save(ResultTermState s) {
        s.setUpdatedAt(Instant.now());
        states.save(s);
    }
}
