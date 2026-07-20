package com.teuprojecto.tracker.notification.application;

import com.teuprojecto.tracker.notification.domain.Notification;
import com.teuprojecto.tracker.notification.domain.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Page<Notification> listForUser(UUID recipientId, Pageable pageable) {
        return notificationRepository.findByRecipientId(recipientId, pageable);
    }
}
