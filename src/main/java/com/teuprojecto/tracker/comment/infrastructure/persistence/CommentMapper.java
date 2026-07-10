package com.teuprojecto.tracker.comment.infrastructure.persistence;

import com.teuprojecto.tracker.comment.domain.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "issueId", source = "issue.id")
    @Mapping(target = "authorId", source = "author.id")
    CommentJpaEntity toJpa(Comment comment);

    @Mapping(target = "issue", ignore = true)
    @Mapping(target = "author", ignore = true)
    Comment toDomain(CommentJpaEntity entity);
}
