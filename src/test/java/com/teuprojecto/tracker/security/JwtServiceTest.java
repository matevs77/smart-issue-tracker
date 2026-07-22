package com.teuprojecto.tracker.security;

import com.teuprojecto.tracker.shared.domain.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "de2b72a8e9c4f61d3a5b8c0d9e7f4a2b6c8d0e1f3a5b7c9d1e3f5a7b9c0d2e4";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 60));
    }

    @Test
    void generateAndValidateReturnsCorrectClaims() {
        var userId = UUID.randomUUID();
        var username = "testuser";
        var role = Role.ADMIN;

        var token = jwtService.generateToken(userId, username, role);
        var claims = jwtService.parseAndValidate(token);

        assertThat(jwtService.extractUsername(claims)).isEqualTo(username);
        assertThat(jwtService.extractUserId(claims)).isEqualTo(userId);
        assertThat(jwtService.extractRoles(claims)).containsExactly(role.name());
    }

    @Test
    void tamperedTokenThrowsInvalidTokenException() {
        var token = jwtService.generateToken(UUID.randomUUID(), "user", Role.VIEWER);
        var tampered = token.substring(0, token.lastIndexOf('.')) + ".tampered";

        assertThatThrownBy(() -> jwtService.parseAndValidate(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void expiredTokenThrowsInvalidTokenException() {
        var secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var past = new Date(System.currentTimeMillis() - 10000);
        var token = Jwts.builder()
                .subject("user")
                .claim("uid", UUID.randomUUID().toString())
                .claim("roles", java.util.List.of("VIEWER"))
                .issuedAt(new Date(past.getTime() - 60000))
                .expiration(past)
                .signWith(secretKey)
                .compact();

        assertThatThrownBy(() -> jwtService.parseAndValidate(token))
                .isInstanceOf(InvalidTokenException.class);
    }
}
