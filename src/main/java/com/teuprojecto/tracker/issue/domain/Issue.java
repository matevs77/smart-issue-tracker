package com.teuprojecto.tracker.issue.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.teuprojecto.tracker.comment.domain.Comment;
import com.teuprojecto.tracker.shared.domain.IssuePriority;
import com.teuprojecto.tracker.shared.domain.IssueStatus;
import com.teuprojecto.tracker.user.domain.User;

public class Issue {

    private final UUID id;
    private String title;
    private String description;
    private IssueStatus status;
    private IssuePriority priority;
    private Double aiConfidenceScore;
    private final User reporter;
    private User assignee;
    private final List<Comment> comments;
    private final Instant createdAt;
    private Instant updatedAt;

    public Issue(UUID id, String title, String description, IssueStatus status, IssuePriority priority,
                 Double aiConfidenceScore, User reporter, User assignee, List<Comment> comments,
                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.aiConfidenceScore = aiConfidenceScore;
        this.reporter = reporter;
        this.assignee = assignee;
        this.comments = comments == null ? new ArrayList<>() : new ArrayList<>(comments);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Issue create(String title, String description, User reporter) {
        var now = Instant.now();
        return new Issue(
            UUID.randomUUID(),
            title,
            description,
            IssueStatus.OPEN,
            null,
            null,
            reporter,
            null,
            List.of(),
            now,
            now
        );
    }

    public void changeStatus(IssueStatus newStatus) {
        if (this.status == IssueStatus.CLOSED) {
            throw new IllegalStateException("Cannot change status of a closed issue");
        }
        if (newStatus == IssueStatus.CLOSED && (description == null || description.isBlank())) {
            throw new IllegalArgumentException("Issue must have a resolution description to be closed (RN-05)");
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void assignTo(User user) {
        this.assignee = user;
        this.updatedAt = Instant.now();
    }

    public void setPriority(IssuePriority priority, Double confidenceScore) {
        this.priority = priority;
        this.aiConfidenceScore = confidenceScore;
        this.updatedAt = Instant.now();
    }

    public void addComment(Comment comment) {
        this.comments.add(comment);
        this.updatedAt = Instant.now();
    }

    public void updateDetails(String title, String description) {
        this.title = title;
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IssueStatus getStatus() { return status; }
    public IssuePriority getPriority() { return priority; }
    public Double getAiConfidenceScore() { return aiConfidenceScore; }
    public User getReporter() { return reporter; }
    public User getAssignee() { return assignee; }
    public List<Comment> getComments() { return Collections.unmodifiableList(comments); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}