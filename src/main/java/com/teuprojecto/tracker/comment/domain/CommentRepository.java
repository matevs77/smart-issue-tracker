package com.teuprojecto.tracker.comment.domain;

import java.util.List;
import java.util.UUID;

public interface CommentRepository {

    Comment save(Comment comment);

    List<Comment> findByIssueId(UUID issueId);
}
