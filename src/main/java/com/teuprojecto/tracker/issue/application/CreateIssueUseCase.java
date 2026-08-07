package com.teuprojecto.tracker.issue.application;

import com.teuprojecto.tracker.issue.domain.Issue;
import com.teuprojecto.tracker.issue.domain.IssueRepository;
import com.teuprojecto.tracker.issue.infrastructure.messaging.IssueEventPublisher;
import com.teuprojecto.tracker.issue.presentation.dto.CreateIssueRequest;
import com.teuprojecto.tracker.user.domain.User;
import com.teuprojecto.tracker.user.domain.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreateIssueUseCase {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final IssueEventPublisher eventPublisher;

    public CreateIssueUseCase(IssueRepository issueRepository, UserRepository userRepository,
                              IssueEventPublisher eventPublisher) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    public Issue execute(CreateIssueRequest request, UUID reporterId) {
        var reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new IllegalArgumentException("Reporter not found"));

        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new IllegalArgumentException("Assignee not found"));
        }

        var issue = Issue.create(request.title(), request.description(), reporter);

        if (assignee != null) {
            issue.assignTo(assignee);
        }

        var savedIssue = issueRepository.save(issue);
        eventPublisher.publishIssueCreated(savedIssue);
        return savedIssue;
    }
}
