package com.teuprojecto.tracker.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
