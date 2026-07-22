package com.teuprojecto.tracker.security.application;

import com.teuprojecto.tracker.security.AuthenticatedUserDetails;
import com.teuprojecto.tracker.security.JwtProperties;
import com.teuprojecto.tracker.security.JwtService;
import com.teuprojecto.tracker.security.presentation.dto.LoginRequest;
import com.teuprojecto.tracker.shared.domain.Role;
import com.teuprojecto.tracker.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtService, new JwtProperties("dummy", 60));
    }

    @Test
    void validCredentialsReturnsAuthResponse() {
        var userId = UUID.randomUUID();
        var user = new User(userId, "user", "user@test.com", "hash", Role.ADMIN, true, Instant.now());
        var userDetails = new AuthenticatedUserDetails(user);
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        var request = new LoginRequest("user", "pass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.generateToken(userId, "user", Role.ADMIN)).thenReturn("token123");

        var response = authService.login(request);

        assertThat(response.token()).isEqualTo("token123");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
    }

    @Test
    void invalidCredentialsThrowsBadCredentialsException() {
        var request = new LoginRequest("user", "wrong");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
