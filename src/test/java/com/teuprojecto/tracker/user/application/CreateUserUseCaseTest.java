package com.teuprojecto.tracker.user.application;

import com.teuprojecto.tracker.shared.domain.Role;
import com.teuprojecto.tracker.user.domain.DuplicateUserException;
import com.teuprojecto.tracker.user.domain.User;
import com.teuprojecto.tracker.user.domain.UserRepository;
import com.teuprojecto.tracker.user.presentation.dto.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CreateUserUseCase createUserUseCase;

    private CreateUserRequest request() {
        return new CreateUserRequest("john", "john@example.com", "password123", "DEVELOPER");
    }

    @Test
    void executeHappyPathEncodesPasswordAndPersistsUser() {
        var request = request();
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var user = createUserUseCase.execute(request);

        assertThat(user.getPasswordHash()).isEqualTo("encoded");
        assertThat(user.getRole()).isEqualTo(Role.DEVELOPER);
        assertThat(user.isActive()).isTrue();
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void executeWithDuplicateUsernameThrowsDuplicateUserException() {
        var request = request();
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThatThrownBy(() -> createUserUseCase.execute(request))
                .isInstanceOf(DuplicateUserException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void executeWithDuplicateEmailThrowsDuplicateUserException() {
        var request = request();
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> createUserUseCase.execute(request))
                .isInstanceOf(DuplicateUserException.class);

        verify(userRepository, never()).save(any());
    }
}
