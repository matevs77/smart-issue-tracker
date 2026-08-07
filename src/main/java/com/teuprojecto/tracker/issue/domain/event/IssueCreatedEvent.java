package com.teuprojecto.tracker.issue.domain.event;

import java.time.Instant;
import java.util.UUID;

import com.teuprojecto.tracker.issue.domain.Issue;
import com.teuprojecto.tracker.shared.event.DomainEvent;

public record IssueCreatedEvent(
        UUID eventId,
        String eventType,
        Instant timestamp,
        IssueCreatedPayload payload) implements DomainEvent {

    public record IssueCreatedPayload(
            UUID issueId,
            String title,
            String description,
            UUID reporterId) {
    }

    public static IssueCreatedEvent from(Issue issue) {
        return new IssueCreatedEvent(
                UUID.randomUUID(),
                "ISSUE_CREATED",
                Instant.now(),
                new IssueCreatedPayload(
                        issue.getId(),
                        issue.getTitle(),
                        issue.getDescription(),
                        issue.getReporter().getId()));
    }
}
