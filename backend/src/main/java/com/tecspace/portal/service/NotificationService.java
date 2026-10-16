package com.tecspace.portal.service;

import com.tecspace.portal.exception.ApiException;
import com.tecspace.portal.model.Notification;
import com.tecspace.portal.model.User;
import com.tecspace.portal.repository.NotificationRepository;
import com.tecspace.portal.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    public void notify(String userId, String type, String title, String message, String senderName) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setSenderName(senderName);
        notifications.save(n);
    }

    public void notifyAll(Collection<String> userIds, String type, String title, String message, String senderName) {
        List<Notification> batch = userIds.stream().distinct().map(id -> {
            Notification n = new Notification();
            n.setUserId(id);
            n.setType(type);
            n.setTitle(title);
            n.setMessage(message);
            n.setSenderName(senderName);
            return n;
        }).toList();
        notifications.saveAll(batch);
    }

    public void notifyAdmins(String type, String title, String message, String senderName) {
        notifyAll(users.findByRole("ROLE_ADMIN").stream().map(User::getId).toList(), type, title, message, senderName);
    }

    public List<Notification> list(String userId) {
        return notifications.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    public long unreadCount(String userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    public void markRead(String userId, String id) {
        Notification n = notifications.findById(id)
                .filter(x -> x.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.isRead()) {
            n.setRead(true);
            notifications.save(n);
        }
    }

    public void markAllRead(String userId) {
        List<Notification> unread = notifications.findByUserIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notifications.saveAll(unread);
    }
}
