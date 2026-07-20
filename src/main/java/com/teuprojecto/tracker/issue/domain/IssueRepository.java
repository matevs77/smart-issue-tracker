package com.teuprojecto.tracker.issue.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface IssueRepository {

    Issue save(Issue issue);

    Optional<Issue> findById(UUID id);

    void deleteById(UUID id);

    Page<Issue> findAll(IssueFilter filter, Pageable pageable);
}
