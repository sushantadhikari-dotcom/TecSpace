package com.tecspace.portal.controller;

import com.tecspace.portal.dto.ContactAdminRequest;
import com.tecspace.portal.model.Notification;
import com.tecspace.portal.model.User;
import com.tecspace.portal.service.NotificationService;
import com.tecspace.portal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** In-app inbox for any logged-in user (student or admin). */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notifications;
    private final UserService userService;

    public NotificationController(NotificationService notifications, UserService userService) {
        this.notifications = notifications;
        this.userService = userService;
    }

    @GetMapping
    public List<Notification> list(Authentication auth) {
        return notifications.list(userService.getByEmail(auth.getName()).getId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(Authentication auth) {
        return Map.of("count", notifications.unreadCount(userService.getByEmail(auth.getName()).getId()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(Authentication auth, @PathVariable String id) {
        notifications.markRead(userService.getByEmail(auth.getName()).getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        notifications.markAllRead(userService.getByEmail(auth.getName()).getId());
        return ResponseEntity.noContent().build();
    }

    /** A student writes to the administration. Every admin gets it in their inbox. */
    @PostMapping("/contact-admin")
    public ResponseEntity<Void> contactAdmin(Authentication auth, @Valid @RequestBody ContactAdminRequest req) {
        User me = userService.getByEmail(auth.getName());
        String sender = (me.getFirstName() + " " + me.getLastName()).trim() + " (" + me.getUniversityEmail() + ")";
        notifications.notifyAdmins("MESSAGE", req.subject().trim(), req.body().trim(), sender);
        return ResponseEntity.noContent().build();
    }
}
