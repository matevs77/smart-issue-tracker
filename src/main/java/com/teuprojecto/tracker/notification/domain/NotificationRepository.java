package com.teuprojecto.tracker.notification.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Page<Notification> findByRecipientId(UUID recipientId, Pageable pageable);
}
