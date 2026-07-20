package com.teuprojecto.tracker.comment.infrastructure.persistence;

import com.teuprojecto.tracker.comment.domain.Comment;
import com.teuprojecto.tracker.comment.domain.CommentRepository;
import com.teuprojecto.tracker.issue.infrastructure.persistence.IssueJpaRepository;
import com.teuprojecto.tracker.issue.infrastructure.persistence.IssueMapper;
import com.teuprojecto.tracker.user.infrastructure.persistence.UserJpaRepository;
import com.teuprojecto.tracker.user.infrastructure.persistence.UserMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CommentRepositoryAdapter implements CommentRepository {

    private final CommentJpaRepository jpaRepository;
    private final CommentMapper commentMapper;
    private final IssueJpaRepository issueJpaRepository;
    private final IssueMapper issueMapper;
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    public CommentRepositoryAdapter(CommentJpaRepository jpaRepository, CommentMapper commentMapper,
                                    IssueJpaRepository issueJpaRepository, IssueMapper issueMapper,
                                    UserJpaRepository userJpaRepository, UserMapper userMapper) {
        this.jpaRepository = jpaRepository;
        this.commentMapper = commentMapper;
        this.issueJpaRepository = issueJpaRepository;
        this.issueMapper = issueMapper;
        this.userJpaRepository = userJpaRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Comment save(Comment comment) {
        var entity = commentMapper.toJpa(comment);
        var saved = jpaRepository.save(entity);
        return toDomainWithAssociations(saved);
    }

    @Override
    public List<Comment> findByIssueId(UUID issueId) {
        return jpaRepository.findByIssueId(issueId).stream()
                .map(this::toDomainWithAssociations)
                .toList();
    }

    private Comment toDomainWithAssociations(CommentJpaEntity entity) {
        var partial = commentMapper.toDomain(entity);
        var issue = issueJpaRepository.findById(entity.getIssueId())
                .map(issueMapper::toDomain)
                .orElse(null);
        var author = userJpaRepository.findById(entity.getAuthorId())
                .map(userMapper::toDomain)
                .orElse(null);
        return new Comment(
                partial.getId(),
                issue,
                author,
                partial.getContent(),
                partial.getCreatedAt()
        );
    }
}
