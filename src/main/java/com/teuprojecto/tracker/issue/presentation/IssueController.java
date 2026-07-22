package com.teuprojecto.tracker.issue.presentation;

import com.teuprojecto.tracker.issue.application.CreateIssueUseCase;
import com.teuprojecto.tracker.issue.application.DeleteIssueUseCase;
import com.teuprojecto.tracker.issue.application.UpdateIssueUseCase;
import com.teuprojecto.tracker.issue.domain.IssueFilter;
import com.teuprojecto.tracker.issue.domain.IssueRepository;
import com.teuprojecto.tracker.issue.presentation.dto.ChangeStatusRequest;
import com.teuprojecto.tracker.issue.presentation.dto.CreateIssueRequest;
import com.teuprojecto.tracker.issue.presentation.dto.IssueResponse;
import com.teuprojecto.tracker.issue.presentation.dto.OverridePriorityRequest;
import com.teuprojecto.tracker.issue.presentation.dto.ReassignRequest;
import com.teuprojecto.tracker.issue.presentation.dto.UpdateDetailsRequest;
import com.teuprojecto.tracker.shared.domain.IssuePriority;
import com.teuprojecto.tracker.shared.domain.IssueStatus;
import com.teuprojecto.tracker.security.AuthenticatedPrincipal;
import com.teuprojecto.tracker.shared.exception.IssueNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final UpdateIssueUseCase updateIssueUseCase;
    private final DeleteIssueUseCase deleteIssueUseCase;
    private final IssueRepository issueRepository;

    public IssueController(CreateIssueUseCase createIssueUseCase, UpdateIssueUseCase updateIssueUseCase,
                           DeleteIssueUseCase deleteIssueUseCase, IssueRepository issueRepository) {
        this.createIssueUseCase = createIssueUseCase;
        this.updateIssueUseCase = updateIssueUseCase;
        this.deleteIssueUseCase = deleteIssueUseCase;
        this.issueRepository = issueRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ResponseEntity<IssueResponse> create(@Valid @RequestBody CreateIssueRequest request,
                                                @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        var issue = createIssueUseCase.execute(request, principal.id());
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

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ResponseEntity<IssueResponse> changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        var issue = updateIssueUseCase.changeStatus(id, request.status());
        return ResponseEntity.ok(IssueResponse.from(issue));
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IssueResponse> overridePriority(@PathVariable UUID id, @Valid @RequestBody OverridePriorityRequest request,
                                                          @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        var issue = updateIssueUseCase.overridePriority(id, request.priority(), principal.username());
        return ResponseEntity.ok(IssueResponse.from(issue));
    }

    @PatchMapping("/{id}/assignee")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ResponseEntity<IssueResponse> reassign(@PathVariable UUID id, @Valid @RequestBody ReassignRequest request) {
        var issue = updateIssueUseCase.reassign(id, request.assigneeId());
        return ResponseEntity.ok(IssueResponse.from(issue));
    }

    @PatchMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    public ResponseEntity<IssueResponse> updateDetails(@PathVariable UUID id, @Valid @RequestBody UpdateDetailsRequest request) {
        var issue = updateIssueUseCase.updateDetails(id, request.title(), request.description());
        return ResponseEntity.ok(IssueResponse.from(issue));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteIssueUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
