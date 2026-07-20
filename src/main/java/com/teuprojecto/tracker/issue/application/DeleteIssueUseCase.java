package com.teuprojecto.tracker.issue.application;

import com.teuprojecto.tracker.issue.domain.IssueRepository;
import com.teuprojecto.tracker.shared.exception.IssueNotFoundException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeleteIssueUseCase {

    private final IssueRepository issueRepository;

    public DeleteIssueUseCase(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    public void execute(UUID id) {
        if (issueRepository.findById(id).isEmpty()) {
            throw new IssueNotFoundException(id);
        }
        issueRepository.deleteById(id);
    }
}
