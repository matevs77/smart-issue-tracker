package com.teuprojecto.tracker.comment.application;

import com.teuprojecto.tracker.comment.domain.Comment;
import com.teuprojecto.tracker.comment.domain.CommentRepository;
import com.teuprojecto.tracker.comment.presentation.dto.CreateCommentRequest;
import com.teuprojecto.tracker.issue.domain.IssueRepository;
import com.teuprojecto.tracker.notification.domain.Notification;
import com.teuprojecto.tracker.notification.domain.NotificationRepository;
import com.teuprojecto.tracker.shared.domain.NotificationType;
import com.teuprojecto.tracker.user.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateCommentUseCase {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public CreateCommentUseCase(CommentRepository commentRepository, IssueRepository issueRepository,
                                UserRepository userRepository, NotificationRepository notificationRepository) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    public Comment execute(UUID issueId, CreateCommentRequest request, UUID authorId) {
        var issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found"));

        var author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Author not found"));

        var comment = Comment.create(issue, author, request.content());
        var saved = commentRepository.save(comment);

        var reporter = issue.getReporter();
        if (reporter != null) {
            var notification = Notification.create(
                reporter,
                "A new comment was added to issue \"" + issue.getTitle() + "\"",
                NotificationType.COMMENT_ADDED
            );
            // TODO(Fase 5): substituir persistência direta por publicação
            // RabbitMQ + NotificationConsumer
            notificationRepository.save(notification);
        }

        return saved;
    }
}
