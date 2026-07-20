package com.teuprojecto.tracker.user.presentation.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    String role,
    boolean active,
    Instant createdAt
) {}
