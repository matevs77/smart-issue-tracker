package com.teuprojecto.tracker.comment.presentation.dto;

import com.teuprojecto.tracker.comment.domain.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID issueId,
    UUID authorId,
    String content,
    Instant createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getIssue().getId(),
            comment.getAuthor().getId(),
            comment.getContent(),
            comment.getCreatedAt()
        );
    }
}
