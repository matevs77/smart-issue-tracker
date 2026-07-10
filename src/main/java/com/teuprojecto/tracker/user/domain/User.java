package com.teuprojecto.tracker.user.domain;

import java.time.Instant;
import java.util.UUID;

import com.teuprojecto.tracker.shared.domain.Role;

public class User {

    private final UUID id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean active;
    private final Instant createdAt;

    public User(UUID id, String username, String email, String passwordHash, Role role, boolean active, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static User create(String username, String email, String passwordHash, Role role) {
        return new User(
            UUID.randomUUID(),
            username,
            email,
            passwordHash,
            role,
            true,
            Instant.now()
        );
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}