package com.teuprojecto.tracker.issue.presentation.dto;

import com.teuprojecto.tracker.issue.domain.Issue;
import com.teuprojecto.tracker.shared.domain.IssuePriority;
import com.teuprojecto.tracker.shared.domain.IssueStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IssueResponse(
    UUID id,
    String title,
    String description,
    IssueStatus status,
    IssuePriority priority,
    Double aiConfidenceScore,
    UserRef reporter,
    UserRef assignee,
    List<CommentEntry> comments,
    Instant createdAt
) {
    public record UserRef(UUID id, String username) {}

    public record CommentEntry(UUID id, String content, Instant createdAt) {}

    public static IssueResponse from(Issue issue) {
        return new IssueResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getAiConfidenceScore(),
                issue.getReporter() != null
                        ? new UserRef(issue.getReporter().getId(), issue.getReporter().getUsername())
                        : null,
                issue.getAssignee() != null
                        ? new UserRef(issue.getAssignee().getId(), issue.getAssignee().getUsername())
                        : null,
                List.of(),
                issue.getCreatedAt()
        );
    }
}
