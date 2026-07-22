package com.teuprojecto.tracker.notification.presentation;

import com.teuprojecto.tracker.notification.application.NotificationService;
import com.teuprojecto.tracker.notification.presentation.dto.NotificationResponse;
import com.teuprojecto.tracker.security.AuthenticatedPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> listForUser(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                                  Pageable pageable) {
        var page = notificationService.listForUser(principal.id(), pageable)
                .map(NotificationResponse::from);
        return ResponseEntity.ok(page);
    }
}
