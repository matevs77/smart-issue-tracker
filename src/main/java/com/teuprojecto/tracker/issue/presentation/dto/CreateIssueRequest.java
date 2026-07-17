package com.teuprojecto.tracker.issue.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateIssueRequest(
    @NotBlank String title,
    @NotBlank String description,
    UUID assigneeId,
    // TODO(Fase 2): remover e extrair do SecurityContext após JWT
    UUID reporterId
) {}
