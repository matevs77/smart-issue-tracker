package com.teuprojecto.tracker.issue.application;

import com.teuprojecto.tracker.issue.domain.Issue;
import com.teuprojecto.tracker.issue.domain.IssueRepository;
import com.teuprojecto.tracker.issue.presentation.dto.CreateIssueRequest;
import com.teuprojecto.tracker.user.domain.User;
import com.teuprojecto.tracker.user.domain.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class CreateIssueUseCase {

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    public CreateIssueUseCase(IssueRepository issueRepository, UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    public Issue execute(CreateIssueRequest request) {
        var reporter = userRepository.findById(request.reporterId())
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

        return issueRepository.save(issue);
    }
}
