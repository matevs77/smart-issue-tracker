package com.teuprojecto.tracker.issue.application;

import com.teuprojecto.tracker.issue.domain.Issue;
import com.teuprojecto.tracker.issue.domain.IssueRepository;
import com.teuprojecto.tracker.issue.presentation.dto.CreateIssueRequest;
import com.teuprojecto.tracker.shared.domain.Role;
import com.teuprojecto.tracker.user.domain.User;
import com.teuprojecto.tracker.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateIssueUseCaseTest {

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CreateIssueUseCase createIssueUseCase;

    @Test
    void executeHappyPathCreatesIssue() {
        var reporterId = UUID.randomUUID();
        var reporter = User.create("reporter", "reporter@example.com", "hash", Role.DEVELOPER);
        var request = new CreateIssueRequest("title", "description", null);

        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

        var issue = createIssueUseCase.execute(request, reporterId);

        assertThat(issue.getTitle()).isEqualTo("title");
        assertThat(issue.getReporter()).isEqualTo(reporter);
        verify(issueRepository).save(any(Issue.class));
    }

    @Test
    void executeWithUnknownReporterThrowsIllegalArgumentException() {
        var reporterId = UUID.randomUUID();
        var request = new CreateIssueRequest("title", "description", null);

        when(userRepository.findById(reporterId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createIssueUseCase.execute(request, reporterId))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
