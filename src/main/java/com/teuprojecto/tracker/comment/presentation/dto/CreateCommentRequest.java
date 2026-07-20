package com.teuprojecto.tracker.comment.presentation.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCommentRequest(
    @NotBlank String content,
    // TODO(Fase 2): remover e extrair do SecurityContext após JWT
    UUID authorId
) {}
