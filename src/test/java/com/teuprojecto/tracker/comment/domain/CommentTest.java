package com.teuprojecto.tracker.comment.domain;

import com.teuprojecto.tracker.issue.domain.Issue;
import com.teuprojecto.tracker.shared.domain.Role;
import com.teuprojecto.tracker.user.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTest {

    private User user(String username) {
        return User.create(username, username + "@example.com", "hash", Role.DEVELOPER);
    }

    @Test
    void createWithAuthorEqualToReporterThrowsIllegalArgumentException() {
        var reporter = user("reporter");
        var issue = Issue.create("title", "description", reporter);

        assertThatThrownBy(() -> Comment.create(issue, reporter, "content"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createWithDifferentAuthorSucceeds() {
        var reporter = user("reporter");
        var issue = Issue.create("title", "description", reporter);
        var author = user("author");

        var comment = Comment.create(issue, author, "content");

        assertThat(comment.getId()).isNotNull();
        assertThat(comment.getAuthor()).isEqualTo(author);
        assertThat(comment.getIssue()).isEqualTo(issue);
        assertThat(comment.getContent()).isEqualTo("content");
    }
}
