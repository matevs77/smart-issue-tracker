package com.teuprojecto.tracker.security;

import com.teuprojecto.tracker.shared.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId, String username, Role role) {
        long expirationMillis = jwtProperties.expirationMinutes() * 60 * 1000L;
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId.toString())
                .claim("roles", List.of(role.name()))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new InvalidTokenException("Token inválido ou expirado", e);
        }
    }

    public String extractUsername(Claims claims) {
        return claims.getSubject();
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.get("uid", String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        return claims.get("roles", List.class);
    }
}
