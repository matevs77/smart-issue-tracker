package com.teuprojecto.tracker.issue.domain;

import com.teuprojecto.tracker.shared.domain.IssuePriority;
import com.teuprojecto.tracker.shared.domain.IssueStatus;
import java.util.UUID;

public record IssueFilter(
    IssueStatus status,
    IssuePriority priority,
    UUID reporterId,
    UUID assigneeId
) {}
