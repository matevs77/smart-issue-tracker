package com.teuprojecto.tracker.issue.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface IssueJpaRepository extends JpaRepository<IssueJpaEntity, UUID>, JpaSpecificationExecutor<IssueJpaEntity> {
}
