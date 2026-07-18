package com.teuprojecto.tracker.user.application;

import com.teuprojecto.tracker.shared.domain.Role;
import com.teuprojecto.tracker.user.domain.DuplicateUserException;
import com.teuprojecto.tracker.user.domain.User;
import com.teuprojecto.tracker.user.domain.UserRepository;
import com.teuprojecto.tracker.user.presentation.dto.CreateUserRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User execute(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email already exists: " + request.getEmail());
        }

        var role = Role.valueOf(request.getRole());
        var passwordHash = passwordEncoder.encode(request.getPassword());

        var user = User.create(request.getUsername(), request.getEmail(), passwordHash, role);

        return userRepository.save(user);
    }
}
