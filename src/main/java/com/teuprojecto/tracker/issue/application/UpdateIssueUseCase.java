package com.teuprojecto.tracker.issue.application;

import com.teuprojecto.tracker.issue.domain.Issue;
import com.teuprojecto.tracker.issue.domain.IssueRepository;
import com.teuprojecto.tracker.shared.domain.IssuePriority;
import com.teuprojecto.tracker.shared.domain.IssueStatus;
import com.teuprojecto.tracker.shared.exception.IssueNotFoundException;
import com.teuprojecto.tracker.user.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class UpdateIssueUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateIssueUseCase.class);

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    public UpdateIssueUseCase(IssueRepository issueRepository, UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    public Issue changeStatus(UUID id, IssueStatus newStatus) {
        var issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));
        issue.changeStatus(newStatus);
        return issueRepository.save(issue);
    }

    public Issue overridePriority(UUID id, IssuePriority priority, String responsibleUsername) {
        var issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));
        var previousPriority = issue.getPriority();
        issue.setPriority(priority, null);
        // RN-03: toda sobreposição manual de prioridade deve ser registada em log com timestamp e responsável
        log.info("AUDIT priority_override issueId={} from={} to={} responsible={} at={}",
                id, previousPriority, priority, responsibleUsername, Instant.now());
        return issueRepository.save(issue);
    }

    public Issue reassign(UUID id, UUID newAssigneeId) {
        var issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));
        var assignee = userRepository.findById(newAssigneeId)
                .orElseThrow(() -> new IllegalArgumentException("Assignee not found"));
        issue.assignTo(assignee);
        return issueRepository.save(issue);
    }

    public Issue updateDetails(UUID id, String title, String description) {
        var issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));
        issue.updateDetails(title, description);
        return issueRepository.save(issue);
    }
}
