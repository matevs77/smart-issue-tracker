package com.teuprojecto.tracker.issue.domain;

import com.teuprojecto.tracker.shared.domain.IssuePriority;
import com.teuprojecto.tracker.shared.domain.IssueStatus;
import com.teuprojecto.tracker.user.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueTest {

    private User reporter() {
        return User.create("reporter", "reporter@example.com", "hash", com.teuprojecto.tracker.shared.domain.Role.DEVELOPER);
    }

    @Test
    void changeStatusToClosedWithoutDescriptionThrowsIllegalArgumentException() {
        var issue = Issue.create("title", "", reporter());

        assertThatThrownBy(() -> issue.changeStatus(IssueStatus.CLOSED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeStatusOnClosedIssueThrowsIllegalStateException() {
        var issue = Issue.create("title", "resolution description", reporter());
        issue.changeStatus(IssueStatus.CLOSED);

        assertThatThrownBy(() -> issue.changeStatus(IssueStatus.OPEN))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void assignToUpdatesAssigneeAndUpdatedAt() {
        var issue = Issue.create("title", "description", reporter());
        var assignee = User.create("assignee", "assignee@example.com", "hash", com.teuprojecto.tracker.shared.domain.Role.DEVELOPER);
        var before = issue.getUpdatedAt();

        issue.assignTo(assignee);

        assertThat(issue.getAssignee()).isEqualTo(assignee);
        assertThat(issue.getUpdatedAt()).isAfter(before);
    }

    @Test
    void setPriorityUpdatesPriorityAndAiConfidenceScore() {
        var issue = Issue.create("title", "description", reporter());

        issue.setPriority(IssuePriority.HIGH, 0.9);

        assertThat(issue.getPriority()).isEqualTo(IssuePriority.HIGH);
        assertThat(issue.getAiConfidenceScore()).isEqualTo(0.9);
    }
}
