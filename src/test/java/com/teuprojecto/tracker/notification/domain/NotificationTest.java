package com.teuprojecto.tracker.notification.domain;

import com.teuprojecto.tracker.shared.domain.NotificationStatus;
import com.teuprojecto.tracker.shared.domain.NotificationType;
import com.teuprojecto.tracker.user.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    private User recipient() {
        return User.create("recipient", "recipient@example.com", "hash", com.teuprojecto.tracker.shared.domain.Role.DEVELOPER);
    }

    private Notification pending() {
        return Notification.create(recipient(), "message", NotificationType.COMMENT_ADDED);
    }

    @Test
    void markAsSentFromPendingSucceeds() {
        var notification = pending();

        notification.markAsSent();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void markAsSentFromSentThrowsIllegalStateException() {
        var notification = pending();
        notification.markAsSent();

        assertThatThrownBy(notification::markAsSent)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markAsSentFromFailedThrowsIllegalStateException() {
        var notification = pending();
        notification.markAsFailed();

        assertThatThrownBy(notification::markAsSent)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markAsFailedFromPendingSucceeds() {
        var notification = pending();

        notification.markAsFailed();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void markAsFailedFromSentThrowsIllegalStateException() {
        var notification = pending();
        notification.markAsSent();

        assertThatThrownBy(notification::markAsFailed)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markAsFailedFromFailedThrowsIllegalStateException() {
        var notification = pending();
        notification.markAsFailed();

        assertThatThrownBy(notification::markAsFailed)
                .isInstanceOf(IllegalStateException.class);
    }
}
