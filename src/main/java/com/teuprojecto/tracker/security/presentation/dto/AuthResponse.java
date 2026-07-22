package com.teuprojecto.tracker.security.presentation.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn
) {}
