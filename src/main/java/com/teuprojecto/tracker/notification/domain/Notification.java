package com.teuprojecto.tracker.notification.domain;

import java.time.Instant;
import java.util.UUID;

import com.teuprojecto.tracker.shared.domain.NotificationStatus;
import com.teuprojecto.tracker.shared.domain.NotificationType;
import com.teuprojecto.tracker.user.domain.User;

public class Notification {

    private final UUID id;
    private final User recipient;
    private final String message;
    private final NotificationType type;
    private NotificationStatus status;
    private final Instant createdAt;

    public Notification(UUID id, User recipient, String message, NotificationType type,
                        NotificationStatus status, Instant createdAt) {
        this.id = id;
        this.recipient = recipient;
        this.message = message;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Notification create(User recipient, String message, NotificationType type) {
        return new Notification(
            UUID.randomUUID(),
            recipient,
            message,
            type,
            NotificationStatus.PENDING,
            Instant.now()
        );
    }

    public void markAsSent() {
        if (this.status != NotificationStatus.PENDING) {
            throw new IllegalStateException("Only pending notifications can be marked as sent");
        }
        this.status = NotificationStatus.SENT;
    }

    public void markAsFailed() {
        if (this.status != NotificationStatus.PENDING) {
            throw new IllegalStateException("Only pending notifications can be marked as failed");
        }
        this.status = NotificationStatus.FAILED;
    }

    public UUID getId() { return id; }
    public User getRecipient() { return recipient; }
    public String getMessage() { return message; }
    public NotificationType getType() { return type; }
    public NotificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}