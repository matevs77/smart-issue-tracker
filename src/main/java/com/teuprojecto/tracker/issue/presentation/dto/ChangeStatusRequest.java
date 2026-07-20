package com.teuprojecto.tracker.issue.presentation.dto;

import com.teuprojecto.tracker.shared.domain.IssueStatus;

public record ChangeStatusRequest(IssueStatus status) {}
