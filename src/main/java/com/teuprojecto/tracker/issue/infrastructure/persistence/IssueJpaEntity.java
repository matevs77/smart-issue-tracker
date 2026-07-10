package com.teuprojecto.tracker.issue.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_issues", indexes = {
    @Index(name = "idx_issues_status", columnList = "status"),
    @Index(name = "idx_issues_reporter", columnList = "reporter_id"),
    @Index(name = "idx_issues_assignee", columnList = "assignee_id"),
    @Index(name = "idx_issues_created_at", columnList = "created_at DESC")
})
@Check(constraints = "status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')")
@Check(constraints = "priority IN ('LOW','MEDIUM','HIGH','CRITICAL')")
@Check(constraints = "ai_confidence_score >= 0.0 AND ai_confidence_score <= 1.0")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class IssueJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 20)
    private String priority;

    @Column(name = "ai_confidence_score")
    private Double aiConfidenceScore;

    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
