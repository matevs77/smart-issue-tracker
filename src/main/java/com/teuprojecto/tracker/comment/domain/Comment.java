package com.teuprojecto.tracker.comment.domain;

import java.time.Instant;
import java.util.UUID;

import com.teuprojecto.tracker.issue.domain.Issue;
import com.teuprojecto.tracker.user.domain.User;

public class Comment {

    private final UUID id;
    private final Issue issue;
    private final User author;
    private final String content;
    private final Instant createdAt;

    public Comment(UUID id, Issue issue, User author, String content, Instant createdAt) {
        this.id = id;
        this.issue = issue;
        this.author = author;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static Comment create(Issue issue, User author, String content) {
        if (author.equals(issue.getReporter())) {
            throw new IllegalArgumentException("Reporter cannot comment on their own issue");
        }
        return new Comment(
            UUID.randomUUID(),
            issue,
            author,
            content,
            Instant.now()
        );
    }

    public UUID getId() { return id; }
    public Issue getIssue() { return issue; }
    public User getAuthor() { return author; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}