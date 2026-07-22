package com.teuprojecto.tracker.issue.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateIssueRequest(
    @NotBlank String title,
    @NotBlank String description,
    UUID assigneeId
) {}
