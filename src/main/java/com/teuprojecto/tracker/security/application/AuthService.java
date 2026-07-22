package com.teuprojecto.tracker.security.application;

import com.teuprojecto.tracker.security.AuthenticatedUserDetails;
import com.teuprojecto.tracker.security.JwtProperties;
import com.teuprojecto.tracker.security.JwtService;
import com.teuprojecto.tracker.security.presentation.dto.AuthResponse;
import com.teuprojecto.tracker.security.presentation.dto.LoginRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    public AuthResponse login(LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        var userDetails = (AuthenticatedUserDetails) auth.getPrincipal();
        var token = jwtService.generateToken(userDetails.getId(), userDetails.getUsername(), userDetails.getRole());
        return new AuthResponse(token, "Bearer", jwtProperties.expirationMinutes() * 60L);
    }
}
