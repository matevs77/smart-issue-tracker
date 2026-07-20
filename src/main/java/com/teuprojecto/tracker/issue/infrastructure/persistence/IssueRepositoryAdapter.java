package com.teuprojecto.tracker.issue.infrastructure.persistence;

import com.teuprojecto.tracker.issue.domain.Issue;
import com.teuprojecto.tracker.issue.domain.IssueFilter;
import com.teuprojecto.tracker.issue.domain.IssueRepository;
import com.teuprojecto.tracker.user.infrastructure.persistence.UserJpaRepository;
import com.teuprojecto.tracker.user.infrastructure.persistence.UserMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class IssueRepositoryAdapter implements IssueRepository {

    private final IssueJpaRepository jpaRepository;
    private final IssueMapper issueMapper;
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    public IssueRepositoryAdapter(IssueJpaRepository jpaRepository, IssueMapper issueMapper,
                                  UserJpaRepository userJpaRepository, UserMapper userMapper) {
        this.jpaRepository = jpaRepository;
        this.issueMapper = issueMapper;
        this.userJpaRepository = userJpaRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Issue save(Issue issue) {
        var entity = issueMapper.toJpa(issue);
        var saved = jpaRepository.save(entity);
        return toDomainWithAssociations(saved);
    }

    @Override
    public Optional<Issue> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomainWithAssociations);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Page<Issue> findAll(IssueFilter filter, Pageable pageable) {
        Specification<IssueJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status().name()));
            }
            if (filter.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.priority().name()));
            }
            if (filter.reporterId() != null) {
                predicates.add(cb.equal(root.get("reporterId"), filter.reporterId()));
            }
            if (filter.assigneeId() != null) {
                predicates.add(cb.equal(root.get("assigneeId"), filter.assigneeId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return jpaRepository.findAll(spec, pageable).map(this::toDomainWithAssociations);
    }

    private Issue toDomainWithAssociations(IssueJpaEntity entity) {
        var partial = issueMapper.toDomain(entity);
        var reporter = userJpaRepository.findById(entity.getReporterId())
                .map(userMapper::toDomain)
                .orElse(null);
        var assignee = entity.getAssigneeId() != null
                ? userJpaRepository.findById(entity.getAssigneeId())
                        .map(userMapper::toDomain)
                        .orElse(null)
                : null;
        return new Issue(
                partial.getId(),
                partial.getTitle(),
                partial.getDescription(),
                partial.getStatus(),
                partial.getPriority(),
                partial.getAiConfidenceScore(),
                reporter,
                assignee,
                List.of(),
                partial.getCreatedAt(),
                partial.getUpdatedAt()
        );
    }
}
