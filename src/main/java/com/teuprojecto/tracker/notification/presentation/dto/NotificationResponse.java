package com.teuprojecto.tracker.notification.presentation.dto;

import com.teuprojecto.tracker.shared.domain.NotificationStatus;
import com.teuprojecto.tracker.shared.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    NotificationType type,
    String message,
    NotificationStatus status,
    Instant createdAt
) {
}
