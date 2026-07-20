package com.teuprojecto.tracker.notification.presentation;

import com.teuprojecto.tracker.notification.application.NotificationService;
import com.teuprojecto.tracker.notification.presentation.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    // TODO(Fase 2): extrair recipientId do SecurityContext (JWT) em vez de query param
    public ResponseEntity<Page<NotificationResponse>> listForUser(@RequestParam UUID recipientId,
                                                                 Pageable pageable) {
        var page = notificationService.listForUser(recipientId, pageable)
                .map(NotificationResponse::from);
        return ResponseEntity.ok(page);
    }
}
