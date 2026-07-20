package com.teuprojecto.tracker.comment.presentation;

import com.teuprojecto.tracker.comment.application.CreateCommentUseCase;
import com.teuprojecto.tracker.comment.domain.CommentRepository;
import com.teuprojecto.tracker.comment.presentation.dto.CommentResponse;
import com.teuprojecto.tracker.comment.presentation.dto.CreateCommentRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issues/{issueId}/comments")
public class CommentController {

    private final CreateCommentUseCase createCommentUseCase;
    private final CommentRepository commentRepository;

    public CommentController(CreateCommentUseCase createCommentUseCase, CommentRepository commentRepository) {
        this.createCommentUseCase = createCommentUseCase;
        this.commentRepository = commentRepository;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(@PathVariable UUID issueId,
                                                  @Valid @RequestBody CreateCommentRequest request) {
        var comment = createCommentUseCase.execute(issueId, request, request.authorId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.from(comment));
    }

    @GetMapping
    public ResponseEntity<Page<CommentResponse>> findByIssue(@PathVariable UUID issueId, Pageable pageable) {
        var page = commentRepository.findByIssueId(issueId).stream()
                .map(CommentResponse::from)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(), list -> new org.springframework.data.domain.PageImpl<>(
                                list, pageable, list.size())));
        return ResponseEntity.ok(page);
    }
}
