package com.teuprojecto.tracker.issue.infrastructure.persistence;

import com.teuprojecto.tracker.issue.domain.Issue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IssueMapper {

    @Mapping(target = "reporterId", source = "reporter.id")
    @Mapping(target = "assigneeId", source = "assignee.id")
    IssueJpaEntity toJpa(Issue issue);

    @Mapping(target = "reporter", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "comments", ignore = true)
    Issue toDomain(IssueJpaEntity entity);
}
