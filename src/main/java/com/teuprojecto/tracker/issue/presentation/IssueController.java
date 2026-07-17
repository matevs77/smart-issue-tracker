package com.teuprojecto.tracker.issue.presentation;

import com.teuprojecto.tracker.issue.application.CreateIssueUseCase;
import com.teuprojecto.tracker.issue.domain.IssueFilter;
import com.teuprojecto.tracker.issue.domain.IssueRepository;
import com.teuprojecto.tracker.issue.presentation.dto.CreateIssueRequest;
import com.teuprojecto.tracker.issue.presentation.dto.IssueResponse;
import com.teuprojecto.tracker.shared.domain.IssuePriority;
import com.teuprojecto.tracker.shared.domain.IssueStatus;
import com.teuprojecto.tracker.shared.exception.IssueNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issues")
public class IssueController {

    private final CreateIssueUseCase createIssueUseCase;
    private final IssueRepository issueRepository;

    public IssueController(CreateIssueUseCase createIssueUseCase, IssueRepository issueRepository) {
        this.createIssueUseCase = createIssueUseCase;
        this.issueRepository = issueRepository;
    }

    @PostMapping
    public ResponseEntity<IssueResponse> create(@Valid @RequestBody CreateIssueRequest request) {
        var issue = createIssueUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(IssueResponse.from(issue));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueResponse> findById(@PathVariable UUID id) {
        var issue = issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(id));
        return ResponseEntity.ok(IssueResponse.from(issue));
    }

    @GetMapping
    public ResponseEntity<Page<IssueResponse>> findAll(
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssuePriority priority,
            @RequestParam(required = false) UUID reporterId,
            @RequestParam(required = false) UUID assigneeId,
            Pageable pageable) {
        var filter = new IssueFilter(status, priority, reporterId, assigneeId);
        var page = issueRepository.findAll(filter, pageable)
                .map(IssueResponse::from);
        return ResponseEntity.ok(page);
    }
}
