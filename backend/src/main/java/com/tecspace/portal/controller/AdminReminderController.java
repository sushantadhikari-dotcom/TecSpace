package com.tecspace.portal.controller;

import com.tecspace.portal.service.ReminderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Administrators only. The same job also runs by itself every morning. */
@RestController
@RequestMapping("/api/admin/reminders")
public class AdminReminderController {

    private final ReminderService reminders;

    public AdminReminderController(ReminderService reminders) {
        this.reminders = reminders;
    }

    @PostMapping("/run")
    public Map<String, Integer> run() {
        return reminders.run();
    }
}
