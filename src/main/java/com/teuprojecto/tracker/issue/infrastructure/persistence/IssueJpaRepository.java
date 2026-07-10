package com.teuprojecto.tracker.issue.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IssueJpaRepository extends JpaRepository<IssueJpaEntity, UUID> {
}
