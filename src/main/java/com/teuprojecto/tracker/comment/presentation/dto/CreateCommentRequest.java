package com.teuprojecto.tracker.comment.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
    @NotBlank String content
) {}
